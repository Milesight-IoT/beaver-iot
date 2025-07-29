package com.milesight.beaveriot.semaphore.local;

import com.google.common.collect.Maps;
import com.milesight.beaveriot.semaphore.DistributedSemaphore;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * author: Luxb
 * create: 2025/7/25 14:31
 **/
public class LocalSemaphore implements DistributedSemaphore {
    private final Map<String, Semaphore> semaphores = Maps.newConcurrentMap();

    @Override
    public void initPermits(String key, int permits) {
        semaphores.put(key, new Semaphore(permits));
    }

    @Override
    public boolean acquire(String key, Duration timeout) {
        Semaphore semaphore = semaphores.get(key);
        try {
            return semaphore.tryAcquire(timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    @Override
    public void release(String key) {
        Semaphore semaphore = semaphores.get(key);
        if (semaphore != null) {
            semaphore.release();
        }
    }
}