package com.milesight.beaveriot.semaphore.redis;

import com.google.common.collect.Maps;
import com.milesight.beaveriot.semaphore.DistributedSemaphore;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * author: Luxb
 * create: 2025/7/25 14:33
 **/
@Slf4j
public class RedisSemaphore implements DistributedSemaphore {
    private static final Duration DEFAULT_PERIOD_WATCH = Duration.ofSeconds(10);
    private static final Duration DEFAULT_DURATION_LEASE = Duration.ofSeconds(DEFAULT_PERIOD_WATCH.getSeconds() * 2);
    private static final Duration DEFAULT_DURATION_IDLE = Duration.ofMinutes(5);
    private static final Duration DEFAULT_PERIOD_MANAGER = Duration.ofMinutes(5);
    private final Map<String, WatchDog> keyWatchDogs;
    private final ScheduledExecutorService sharedLeaseReNewer;
    private final ScheduledExecutorService watchDogManager;
    private final RedissonClient redissonClient;

    public RedisSemaphore(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        this.keyWatchDogs = Maps.newConcurrentMap();
        this.sharedLeaseReNewer = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        this.watchDogManager = Executors.newSingleThreadScheduledExecutor();
        this.watchDogManager.scheduleAtFixedRate(() -> keyWatchDogs.values().forEach(WatchDog::checkIfIdle),
                0, DEFAULT_PERIOD_MANAGER.toMinutes(), TimeUnit.MINUTES);
    }

    @Override
    public void initPermits(String key, int permits) {
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(key);
        semaphore.trySetPermits(permits);
    }

    @Override
    public String acquire(String key, Duration timeout) {
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(key);
        try {
            String permitId = semaphore.tryAcquire(timeout.toMillis(), DEFAULT_DURATION_LEASE.toMillis(), TimeUnit.MILLISECONDS);
            if (permitId != null) {
                startWatchDog(semaphore, permitId);
            }
            return permitId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private void startWatchDog(RPermitExpirableSemaphore semaphore, String permitId) {
        WatchDog watchDog = keyWatchDogs.computeIfAbsent(semaphore.getName(), k -> WatchDog.create(sharedLeaseReNewer, semaphore));
        watchDog.addPermitId(permitId);
    }

    @Override
    public void release(String key, String permitId) {
        WatchDog watchDog = keyWatchDogs.get(key);
        if (watchDog != null) {
            watchDog.removePermitId(permitId);
        }

        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(key);
        semaphore.release(permitId);
    }

    @PreDestroy
    public void destroy() {
        for (WatchDog watchDog : keyWatchDogs.values()) {
            watchDog.clearPermitIds();
            watchDog.stop(true);
        }
        keyWatchDogs.clear();

        destroyTask(watchDogManager);
        destroyTask(sharedLeaseReNewer);
    }

    private void destroyTask(ScheduledExecutorService service) {
        if (!service.isShutdown()) {
            service.shutdown();
            try {
                if (!service.awaitTermination(5, TimeUnit.SECONDS)) {
                    service.shutdownNow();
                }
            } catch (InterruptedException e) {
                service.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Data
    public static class WatchDog {
        private Set<String> permitIds;
        private RPermitExpirableSemaphore semaphore;
        private ScheduledExecutorService sharedLeaseReNewer;
        private volatile ScheduledFuture<?> future;
        private volatile long lastActiveTime;
        private final ReentrantLock lock;
        private final Duration idleTimeout;

        private WatchDog() {
            lock = new ReentrantLock();
            idleTimeout = DEFAULT_DURATION_IDLE;
            updateLastActiveTime();
        }

        public static WatchDog create(ScheduledExecutorService sharedLeaseReNewer, RPermitExpirableSemaphore semaphore) {
            WatchDog watchDog = new WatchDog();
            watchDog.setSemaphore(semaphore);
            watchDog.setPermitIds(ConcurrentHashMap.newKeySet());
            watchDog.setSharedLeaseReNewer(sharedLeaseReNewer);
            watchDog.start();
            return watchDog;
        }

        public void start() {
            if (future != null) {
                return;
            }

            lock.lock();
            try {
                // Double-check: ensure future is still null before starting
                if (future == null) {
                    future = sharedLeaseReNewer.scheduleAtFixedRate(() -> permitIds.forEach(permitId -> {
                        try {
                            semaphore.updateLeaseTime(permitId, DEFAULT_DURATION_LEASE.toMillis(), TimeUnit.MILLISECONDS);
                        } catch (Exception e) {
                            log.warn("Failed to renew lease for semaphore {} permit: {}", semaphore.getName(), permitId, e);
                        }
                    }), 0, DEFAULT_PERIOD_WATCH.toMillis(), TimeUnit.MILLISECONDS);
                }
            } finally {
                lock.unlock();
            }
        }

        private void updateLastActiveTime() {
            lastActiveTime = System.currentTimeMillis();
        }

        public void addPermitId(String permitId) {
            permitIds.add(permitId);
            updateLastActiveTime();
            start();
        }

        public void removePermitId(String permitId) {
            permitIds.remove(permitId);
            updateLastActiveTime();
        }

        public void clearPermitIds() {
            permitIds.clear();
        }

        public boolean isPermitsEmpty() {
            return permitIds.isEmpty();
        }

        public boolean isIdle() {
            return System.currentTimeMillis() - getLastActiveTime() > idleTimeout.toMillis();
        }

        public void stop() {
            stop(false);
        }

        public void stop(boolean isForce) {
            if (future == null) {
                return;
            }

            lock.lock();
            try {
                // Double-check: ensure it's still idle and permits empty before stopping
                if (isForce || isIdle() && isPermitsEmpty()) {
                    if (future != null && !future.isCancelled()) {
                        future.cancel(false);
                        future = null;
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        public void checkIfIdle() {
            try {
                if (isIdle() && isPermitsEmpty()) {
                    stop();
                }
            } catch (Exception e) {
                log.error("Error occurred while checking if the watchdog is idle for semaphore {}", semaphore.getName(), e);
            }
        }
    }
}