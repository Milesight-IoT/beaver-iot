package com.milesight.beaveriot.semaphore.redis;

import com.milesight.beaveriot.semaphore.DistributedSemaphore;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;

import java.time.Duration;

/**
 * author: Luxb
 * create: 2025/7/25 14:33
 **/
public class RedisSemaphore implements DistributedSemaphore {
    private final RedissonClient redissonClient;

    public RedisSemaphore(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void initPermits(String key, int permits) {
        RSemaphore semaphore = redissonClient.getSemaphore(key);
        semaphore.trySetPermits(permits);
    }

    @Override
    public boolean acquire(String key, Duration timeout) {
        RSemaphore semaphore = redissonClient.getSemaphore(key);
        try {
            return semaphore.tryAcquire(timeout);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    @Override
    public void release(String key) {
        RSemaphore semaphore = redissonClient.getSemaphore(key);
        semaphore.release();
    }
}