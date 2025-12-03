package com.milesight.beaveriot.base.pool;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Generic object pool implementation with lifecycle management
 * <p>
 * Thread-safe object pool that manages creation, borrowing, returning, and destruction of objects.
 * Supports automatic eviction of idle objects while maintaining a minimum pool size.
 * <p>
 *
 * @param <T> the type of objects managed by this pool
 * @author Luxb
 * @date 2025/11/27
 */
@Slf4j
public class ObjectPool<T> implements DisposableBean {
    private final PoolConfig config;
    private final Supplier<T> objectFactory;
    private final Consumer<T> objectDestructor;
    private final Queue<PooledObject<T>> idleObjects;
    private final Map<T, PooledObject<T>> objectMap;
    private final AtomicInteger totalObjects;
    private final ReentrantLock lock;
    private final Condition notEmpty;
    private final ScheduledExecutorService evictionScheduler;
    private final String poolName;
    private volatile boolean closed;

    @SuppressWarnings("unused")
    public static <T> ObjectPool<T> newPool(PoolConfig config,
                                            Supplier<T> objectFactory,
                                            Class<T> objectClass) {
        return newPool(config, objectFactory, null, objectClass);
    }

    public static <T> ObjectPool<T> newPool(PoolConfig config,
                                            Supplier<T> objectFactory,
                                            Consumer<T> objectDestructor,
                                            Class<T> objectClass) {
        return new ObjectPool<>(config, objectFactory, objectDestructor, objectClass);
    }

    /**
     * Create a new object pool
     *
     * @param config           pool configuration
     * @param objectFactory    factory for creating objects
     * @param objectDestructor destructor for destroying objects
     */
    private ObjectPool(PoolConfig config,
                       Supplier<T> objectFactory,
                       Consumer<T> objectDestructor,
                       Class<T> objectClass) {
        this.config = config;
        this.objectFactory = objectFactory;
        this.objectDestructor = objectDestructor;
        this.idleObjects = new ConcurrentLinkedQueue<>();
        this.objectMap = new ConcurrentHashMap<>();
        this.totalObjects = new AtomicInteger(0);
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();
        this.closed = false;
        this.poolName = objectClass.getSimpleName();

        // Initialize min idle objects
        initializeMinIdleObjects();

        // Start eviction scheduler
        this.evictionScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "ObjectPool-Eviction-Scheduler");
            thread.setDaemon(true);
            return thread;
        });

        scheduleEviction();

        log.debug("Pool({}) - Initialized: minIdle={}, maxTotal={}, maxIdleTime={}, evictionInterval={}", poolName,
                config.getMinIdle(), config.getMaxTotal(), config.getMaxIdleTime(), config.getEvictionCheckInterval());
    }

    /**
     * Initialize minimum number of idle objects
     */
    private void initializeMinIdleObjects() {
        for (int i = 0; i < config.getMinIdle(); i++) {
            try {
                createObject(idleObjects::offer);
            } catch (Exception e) {
                log.error("Failed to initialize object #{}", i, e);
            }
        }
        if (totalObjects.get() == config.getMinIdle()) {
            log.debug("Pool({}) - Initialized {} idle objects", poolName, idleObjects.size());
        } else {
            log.warn("Pool({}) - Initialized {} idle objects, but minimum expected is {}", poolName, idleObjects.size(), config.getMinIdle());
        }
    }

    /**
     * Borrow an object from the pool
     *
     * @return borrowed object
     * @throws InterruptedException  if interrupted while waiting
     * @throws TimeoutException      if wait timeout exceeded
     * @throws IllegalStateException if pool is closed
     */
    private T borrowObject() throws InterruptedException, TimeoutException {
        if (closed) {
            throw new IllegalStateException("Pool is closed");
        }

        long deadline = System.currentTimeMillis() + config.getMaxWaitTime().toMillis();

        while (true) {
            // Try to get an idle object
            PooledObject<T> pooledObject = idleObjects.poll();

            if (pooledObject != null && pooledObject.markInUse()) {
                log.debug("Pool({}) - Borrowed existing object from pool. Idle: {}, Total: {}", poolName,
                        idleObjects.size(), totalObjects.get());
                return pooledObject.getObject();
            }

            // Try to create a new object if under max limit
            if (totalObjects.get() < config.getMaxTotal()) {
                lock.lock();
                try {
                    // Double check after acquiring lock
                    if (totalObjects.get() < config.getMaxTotal()) {
                        pooledObject = createObject();
                        if (pooledObject != null && pooledObject.markInUse()) {
                            log.debug("Pool({}) - Created new object. Idle: {}, Total: {}", poolName,
                                    idleObjects.size(), totalObjects.get());
                            return pooledObject.getObject();
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }

            // Wait for an object to be returned
            long remainingTime = deadline - System.currentTimeMillis();
            if (remainingTime <= 0) {
                throw new TimeoutException(MessageFormat.format("ObjectPool({0}) - Timeout waiting for object from pool", poolName));
            }

            lock.lock();
            try {
                log.debug("Pool({}) - Waiting for object to be returned. Idle: {}, Total: {}", poolName,
                        idleObjects.size(), totalObjects.get());
                if (!notEmpty.await(remainingTime, TimeUnit.MILLISECONDS)) {
                    throw new TimeoutException(MessageFormat.format("ObjectPool({0}) - Timeout waiting for object from pool", poolName));
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Return an object to the pool
     *
     * @param object object to return
     */
    private void returnObject(T object) {
        if (object == null) {
            return;
        }

        if (closed) {
            destroyObject(object);
            return;
        }

        PooledObject<T> pooledObject = objectMap.get(object);
        if (pooledObject != null && pooledObject.isInUse()) {
            pooledObject.markAvailable();
            idleObjects.offer(pooledObject);
            log.debug("Pool({}) - Returned object to pool. Idle: {}, Total: {}", poolName,
                    idleObjects.size(), totalObjects.get());
            signalNotEmpty();
        }
    }

    /**
     * Create a new object
     * MUST be called with lock held or during initialization
     */
    private PooledObject<T> createObject() {
        return createObject(null);
    }

    /**
     * Create a new object
     * MUST be called with lock held or during initialization
     * @param createdConsumer consumer to accept the created object
     */
    private PooledObject<T> createObject(Consumer<PooledObject<T>> createdConsumer) {
        try {
            T object = objectFactory.get();
            AtomicBoolean isCreated = new AtomicBoolean(false);
            PooledObject<T> pooledObject = objectMap.computeIfAbsent(object, k -> {
                totalObjects.incrementAndGet();
                isCreated.set(true);
                log.debug("Pool({}) - Created new object instance. Total: {}", poolName, totalObjects.get());
                return new PooledObject<>(object);
            });

            if (isCreated.get() && createdConsumer != null) {
                createdConsumer.accept(pooledObject);
            }
            return pooledObject;
        } catch (Exception e) {
            log.error("Pool({}) - Failed to create object", poolName, e);
            return null;
        }
    }

    /**
     * Destroy an object
     * MUST be called with lock held to avoid race condition with borrowObject
     */
    private void destroyObject(T object) {
        if (object == null) {
            return;
        }

        lock.lock();
        try {
            doDestroyObject(object);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Actually destroy the object (internal method, must hold lock)
     */
    private void doDestroyObject(T object) {
        try {
            if (objectDestructor != null) {
                objectDestructor.accept(object);
            }

            objectMap.remove(object);

            totalObjects.decrementAndGet();
            log.debug("Pool({}) - Destroyed object instance. Total: {}", poolName, totalObjects.get());
        } catch (Exception e) {
            log.error("Pool({}) - Failed to destroy object", poolName, e);
        }
    }

    /**
     * Schedule eviction task
     */
    private void scheduleEviction() {
        evictionScheduler.scheduleWithFixedDelay(
                this::evictIdleObjects,
                config.getEvictionCheckInterval().toMillis(),
                config.getEvictionCheckInterval().toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Evict idle objects that exceed max idle time
     * Keep at least minIdle objects in the pool
     * <p>
     * IMPORTANT: Destroy objects with lock held to avoid race condition
     */
    private void evictIdleObjects() {
        if (closed) {
            return;
        }

        long maxIdleMillis = config.getMaxIdleTime().toMillis();
        int currentSize = idleObjects.size();
        int evicted = 0;

        log.debug("Pool({}) - Starting eviction check. Idle: {}, Total: {}",
                poolName, currentSize, totalObjects.get());

        List<PooledObject<T>> toEvict = new ArrayList<>();

        while (currentSize > config.getMinIdle()) {
            PooledObject<T> pooledObject = idleObjects.peek();
            if (pooledObject == null) {
                break;
            }

            if (!pooledObject.isInUse() && pooledObject.getIdleTimeMillis() > maxIdleMillis) {
                if (idleObjects.remove(pooledObject)) {
                    toEvict.add(pooledObject);
                    currentSize--;
                }
            } else {
                break;
            }
        }

        if (!toEvict.isEmpty()) {
            lock.lock();
            try {
                for (PooledObject<T> pooledObject : toEvict) {
                    doDestroyObject(pooledObject.getObject());
                    evicted++;
                }
            } finally {
                lock.unlock();
            }
            log.debug("Pool({}) - Evicted {} idle objects. Idle: {}, Total: {}",
                    poolName, evicted, idleObjects.size(), totalObjects.get());
        }
    }

    /**
     * Signal waiting threads that an object is available
     */
    private void signalNotEmpty() {
        lock.lock();
        try {
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get current pool statistics
     */
    @SuppressWarnings("unused")
    public PoolStatistics getStatistics() {
        return new PoolStatistics(
                totalObjects.get(),
                idleObjects.size(),
                totalObjects.get() - idleObjects.size(),
                config.getMaxTotal()
        );
    }

    /**
     * Execute an operation with a borrowed object
     * The object will be automatically returned after use
     *
     * @param operation the operation to execute
     * @param <R>       the result type
     * @return the operation result
     */
    @SneakyThrows
    public <R> R execute(Function<T, R> operation) {
        T object = borrowObject();
        try {
            return operation.apply(object);
        } finally {
            returnObject(object);
        }
    }

    /**
     * Execute an operation with a borrowed object
     * The object will be automatically returned after use
     *
     * @param operation the operation to execute
     */
    @SneakyThrows
    public void execute(Consumer<T> operation) {
        T object = borrowObject();
        try {
            operation.accept(object);
        } finally {
            returnObject(object);
        }
    }

    private void close() {
        if (closed) {
            return;
        }

        closed = true;
        log.debug("Pool({}) - Closing ObjectPool...", poolName);

        // Shutdown eviction scheduler
        evictionScheduler.shutdown();
        try {
            if (!evictionScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                evictionScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            evictionScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Destroy all idle objects with lock held
        lock.lock();
        try {
            PooledObject<T> pooledObject;
            while ((pooledObject = idleObjects.poll()) != null) {
                doDestroyObject(pooledObject.getObject());
            }
        } finally {
            lock.unlock();
        }

        log.debug("Pool({}) - ObjectPool closed. Final total objects: {}", poolName, totalObjects.get());
    }

    /**
     * Implementation of DisposableBean for Spring integration
     * Delegates to {@link #close()} method
     */
    @Override
    public void destroy() {
        close();
    }

    /**
     * Pool statistics
     */
    public record PoolStatistics(int totalObjects, int idleObjects, int activeObjects, int maxTotal) {
        @Override
        public String toString() {
            return String.format("PoolStatistics{total=%d, idle=%d, active=%d, max=%d}",
                    totalObjects, idleObjects, activeObjects, maxTotal);
        }
    }
}