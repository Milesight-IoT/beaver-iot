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
    public String acquire(String key, Duration timeout) {
        Semaphore semaphore = semaphores.get(key);
        try {
            return semaphore.tryAcquire(timeout.toNanos(), TimeUnit.NANOSECONDS) ? key : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    @Override
    public void release(String key, String permitId) {
        Semaphore semaphore = semaphores.get(key);
        if (semaphore != null) {
            semaphore.release();
        }
    }
}