package com.milesight.beaveriot.semaphore.redis;

import com.google.common.collect.Maps;
import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.semaphore.DistributedSemaphore;
import jakarta.annotation.PreDestroy;
import lombok.Data;
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
public class RedisSemaphore implements DistributedSemaphore {
    private static final long DEFAULT_LEASE_TIME = 15000;
    private static final long DEFAULT_WATCH_PERIOD = 10000;
    private static final Map<String, WatchDog> keyWatchDogs = Maps.newConcurrentMap();
    private final RedissonClient redissonClient;

    public RedisSemaphore(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
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
                startWatchDog(key, permitId);
            }
            return permitId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private void startWatchDog(String key, String permitId) {
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(key);
        WatchDog watchDog = keyWatchDogs.computeIfAbsent(key, k -> WatchDog.create(semaphore));
        watchDog.getPermitIds().add(permitId);
    }

    @Override
    public void release(String key, String permitId) {
        if (keyWatchDogs.containsKey(key)) {
            WatchDog watchDog = keyWatchDogs.get(key);
            watchDog.getPermitIds().remove(permitId);
        }

        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(key);
        semaphore.release(permitId);
    }

    @PreDestroy
    public void destroy() {
        for (WatchDog watchDog : keyWatchDogs.values()) {
            watchDog.getLeaseReNewer().shutdown();
        }
    }

    @Data
    public static class WatchDog {
        private Set<String> permitIds;
        private ScheduledExecutorService leaseReNewer;
        private RPermitExpirableSemaphore semaphore;

        public static WatchDog create(RPermitExpirableSemaphore semaphore) {
            WatchDog watchDog = new WatchDog();
            watchDog.setSemaphore(semaphore);
            watchDog.setPermitIds(ConcurrentHashMap.newKeySet());
            ScheduledExecutorService leaseReNewer = Executors.newSingleThreadScheduledExecutor();
            leaseReNewer.scheduleAtFixedRate(() -> watchDog.getPermitIds().forEach(eachPermitId ->
                    semaphore.updateLeaseTime(eachPermitId, DEFAULT_LEASE_TIME, TimeUnit.MILLISECONDS)),
                    0, DEFAULT_WATCH_PERIOD, TimeUnit.MILLISECONDS);
            watchDog.setLeaseReNewer(leaseReNewer);
            return watchDog;
        }
    }
}