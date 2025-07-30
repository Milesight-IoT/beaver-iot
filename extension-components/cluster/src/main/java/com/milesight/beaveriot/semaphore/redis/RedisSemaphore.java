package com.milesight.beaveriot.semaphore.redis;

import com.google.common.collect.Maps;
import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.semaphore.DistributedSemaphore;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * author: Luxb
 * create: 2025/7/25 14:33
 **/
@Slf4j
public class RedisSemaphore implements DistributedSemaphore {
    private static final long DEFAULT_LEASE_TIME = 15000;
    private static final long DEFAULT_WATCH_PERIOD = 10000;
    private final Map<String, WatchDog> keyWatchDogs;
    private final ScheduledExecutorService sharedLeaseReNewer;
    private final RedissonClient redissonClient;

    public RedisSemaphore(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        this.keyWatchDogs = Maps.newConcurrentMap();
        this.sharedLeaseReNewer = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @DistributedLock(name = "semaphore-init-#{#p0}", lockAtLeastFor = "0s", lockAtMostFor = "1s", scope = LockScope.GLOBAL, throwOnLockFailure = false)
    @Override
    public void initPermits(String key, int permits) {
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(key);
        semaphore.trySetPermits(permits);
    }

    @Override
    public String acquire(String key, Duration timeout) {
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(key);
        try {
            String permitId = semaphore.tryAcquire(timeout.toMillis(), DEFAULT_LEASE_TIME, TimeUnit.MILLISECONDS);
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
        }
        keyWatchDogs.clear();
        if (!sharedLeaseReNewer.isShutdown()) {
            sharedLeaseReNewer.shutdown();
            try {
                if (!sharedLeaseReNewer.awaitTermination(5, TimeUnit.SECONDS)) {
                    sharedLeaseReNewer.shutdownNow();
                }
            } catch (InterruptedException e) {
                sharedLeaseReNewer.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Data
    public static class WatchDog {
        private Set<String> permitIds;
        private RPermitExpirableSemaphore semaphore;

        private WatchDog() {}

        public static WatchDog create(ScheduledExecutorService sharedLeaseReNewer, RPermitExpirableSemaphore semaphore) {
            WatchDog watchDog = new WatchDog();
            watchDog.setSemaphore(semaphore);
            watchDog.setPermitIds(ConcurrentHashMap.newKeySet());
            sharedLeaseReNewer.scheduleAtFixedRate(() -> watchDog.getPermitIds().forEach(permitId -> {
                try {
                    semaphore.updateLeaseTime(permitId, DEFAULT_LEASE_TIME, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    log.warn("Failed to renew lease for semaphore {} permit: {}", semaphore.getName(), permitId, e);
                }
            }), 0, DEFAULT_WATCH_PERIOD, TimeUnit.MILLISECONDS);
            return watchDog;
        }

        public void addPermitId(String permitId) {
            this.permitIds.add(permitId);
        }

        public void removePermitId(String permitId) {
            this.permitIds.remove(permitId);
        }

        public void clearPermitIds() {
            this.permitIds.clear();
        }
    }
}