package com.milesight.beaveriot.base.pool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
    private final ObjectFactory<T> objectFactory;
    private final ObjectDestructor<T> objectDestructor;
    private final ConcurrentLinkedQueue<PooledObject<T>> idleObjects;
    private final AtomicInteger totalObjects;
    private final ReentrantLock lock;
    private final Condition notEmpty;
    private final ScheduledExecutorService evictionScheduler;
    private volatile boolean closed;

    public static <T> ObjectPool<T> newPool(PoolConfig config,
                                            ObjectFactory<T> objectFactory,
                                            ObjectDestructor<T> objectDestructor) {
        return new ObjectPool<>(config, objectFactory, objectDestructor);
    }

    /**
     * Create a new object pool
     *
     * @param config           pool configuration
     * @param objectFactory    factory for creating objects
     * @param objectDestructor destructor for destroying objects
     */
    private ObjectPool(PoolConfig config,
                      ObjectFactory<T> objectFactory,
                      ObjectDestructor<T> objectDestructor) {
        this.config = config;
        this.objectFactory = objectFactory;
        this.objectDestructor = objectDestructor;
        this.idleObjects = new ConcurrentLinkedQueue<>();
        this.totalObjects = new AtomicInteger(0);
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();
        this.closed = false;

        // Initialize min idle objects
        initializeMinIdleObjects();

        // Start eviction scheduler
        this.evictionScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "ObjectPool-Eviction-Scheduler");
            thread.setDaemon(true);
            return thread;
        });

        scheduleEviction();

        log.debug("ObjectPool initialized: minIdle={}, maxTotal={}, maxIdleTime={}, evictionInterval={}",
                config.getMinIdle(), config.getMaxTotal(), config.getMaxIdleTime(), config.getEvictionCheckInterval());
    }

    /**
     * Create pool with default no-op destructor
     */
    public ObjectPool(PoolConfig config, ObjectFactory<T> objectFactory) {
        this(config, objectFactory, ObjectDestructor.noOp());
    }

    /**
     * Initialize minimum number of idle objects
     */
    private void initializeMinIdleObjects() {
        for (int i = 0; i < config.getMinIdle(); i++) {
            try {
                PooledObject<T> pooledObject = createObject();
                if (pooledObject != null) {
                    idleObjects.offer(pooledObject);
                }
            } catch (Exception e) {
                log.error("Failed to initialize object #{}", i, e);
            }
        }
        log.debug("Initialized {} idle objects", idleObjects.size());
    }

    /**
     * Borrow an object from the pool
     *
     * @return borrowed object
     * @throws InterruptedException  if interrupted while waiting
     * @throws TimeoutException      if wait timeout exceeded
     * @throws IllegalStateException if pool is closed
     */
    public T borrowObject() throws InterruptedException, TimeoutException {
        if (closed) {
            throw new IllegalStateException("Pool is closed");
        }

        long deadline = System.currentTimeMillis() + config.getMaxWaitTime().toMillis();

        while (true) {
            // Try to get an idle object
            PooledObject<T> pooledObject = idleObjects.poll();

            if (pooledObject != null && pooledObject.markInUse()) {
                log.debug("Borrowed existing object from pool. Idle: {}, Total: {}",
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
                            log.debug("Created new object. Idle: {}, Total: {}",
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
                throw new TimeoutException("Timeout waiting for object from pool");
            }

            lock.lock();
            try {
                log.debug("Waiting for object to be returned. Idle: {}, Total: {}",
                        idleObjects.size(), totalObjects.get());
                if (!notEmpty.await(remainingTime, TimeUnit.MILLISECONDS)) {
                    throw new TimeoutException("Timeout waiting for object from pool");
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
    public void returnObject(T object) {
        if (object == null) {
            return;
        }

        if (closed) {
            destroyObject(object);
            return;
        }

        // Find the corresponding PooledObject
        for (PooledObject<T> pooledObject : idleObjects) {
            if (pooledObject.getObject() == object) {
                pooledObject.markAvailable();
                signalNotEmpty();
                log.debug("Object already in idle queue, marked as available");
                return;
            }
        }

        // Create a PooledObject wrapper if not found (shouldn't happen normally)
        PooledObject<T> pooledObject = new PooledObject<>(object);
        pooledObject.markAvailable();
        idleObjects.offer(pooledObject);
        signalNotEmpty();

        log.debug("Returned object to pool. Idle: {}, Total: {}",
                idleObjects.size(), totalObjects.get());
    }

    /**
     * Create a new object
     * MUST be called with lock held or during initialization
     */
    private PooledObject<T> createObject() {
        try {
            T object = objectFactory.create();
            totalObjects.incrementAndGet();
            log.debug("Created new object instance. Total: {}", totalObjects.get());
            return new PooledObject<>(object);
        } catch (Exception e) {
            log.error("Failed to create object", e);
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
            objectDestructor.destroy(object);
            totalObjects.decrementAndGet();
            log.debug("Destroyed object instance. Total: {}", totalObjects.get());
        } catch (Exception e) {
            log.error("Failed to destroy object", e);
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

        log.debug("Starting eviction check. Idle: {}, Total: {}", currentSize, totalObjects.get());

        while (currentSize > config.getMinIdle()) {
            PooledObject<T> pooledObject = idleObjects.peek();

            if (pooledObject == null) {
                break;
            }

            // Only evict if object is not in use and has been idle for too long
            if (!pooledObject.isInUse() && pooledObject.getIdleTimeMillis() > maxIdleMillis) {
                // Remove from queue first
                if (idleObjects.remove(pooledObject)) {
                    // Then destroy with lock to avoid race condition with borrowObject
                    lock.lock();
                    try {
                        doDestroyObject(pooledObject.getObject());
                    } finally {
                        lock.unlock();
                    }

                    evicted++;
                    currentSize--;
                    log.debug("Evicted idle object. Idle time: {}ms", pooledObject.getIdleTimeMillis());
                }
            } else {
                break; // Objects are roughly ordered by last used time
            }
        }

        if (evicted > 0) {
            log.debug("Evicted {} idle objects. Idle: {}, Total: {}",
                    evicted, idleObjects.size(), totalObjects.get());
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
     * @throws Exception if operation fails or timeout
     */
    public <R> R execute(PooledObjectWithResultOperation<T, R> operation) throws Exception {
        T object = borrowObject();
        try {
            return operation.execute(object);
        } finally {
            returnObject(object);
        }
    }

    public void execute(PooledObjectWithoutResultOperation<T> operation) throws Exception {
        T object = borrowObject();
        try {
            operation.execute(object);
        } finally {
            returnObject(object);
        }
    }

    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        log.debug("Closing ObjectPool...");

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

        log.debug("ObjectPool closed. Final total objects: {}", totalObjects.get());
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
     * Functional interface for operations on pooled objects
     */
    @FunctionalInterface
    public interface PooledObjectWithResultOperation<T, R> {
        R execute(T object) throws Exception;
    }

    @FunctionalInterface
    public interface PooledObjectWithoutResultOperation<T> {
        void execute(T object) throws Exception;
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