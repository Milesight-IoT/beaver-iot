package com.milesight.beaveriot.semaphore.redis;

import com.google.common.collect.Maps;
import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.semaphore.DistributedSemaphore;
import jakarta.annotation.PreDestroy;
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
    private static final Map<String, ScheduledExecutorService> keyWatchDogs = Maps.newConcurrentMap();
    private static final Map<String, Set<String>> keyPermitIds = Maps.newConcurrentMap();
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
        Set<String> permitIds = keyPermitIds.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
        permitIds.add(permitId);

        if (!keyWatchDogs.containsKey(key)) {
            RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(key);
            ScheduledExecutorService watchDog = Executors.newSingleThreadScheduledExecutor();
            watchDog.scheduleAtFixedRate(() -> {
                Set<String> thePermitIds = keyPermitIds.get(key);
                thePermitIds.forEach(eachPermitId -> semaphore.updateLeaseTime(eachPermitId, DEFAULT_LEASE_TIME, TimeUnit.MILLISECONDS));
            }, 0, DEFAULT_WATCH_PERIOD, TimeUnit.MILLISECONDS);
            keyWatchDogs.put(key, watchDog);
        }
    }

    @Override
    public void release(String key, String permitId) {
        if (keyPermitIds.containsKey(key)) {
            Set<String> permitIds = keyPermitIds.get(key);
            permitIds.remove(permitId);
        }

        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(key);
        semaphore.release(permitId);
    }

    @PreDestroy
    public void destroy() {
        for (ScheduledExecutorService watchDog : keyWatchDogs.values()) {
            watchDog.shutdown();
        }
    }
}