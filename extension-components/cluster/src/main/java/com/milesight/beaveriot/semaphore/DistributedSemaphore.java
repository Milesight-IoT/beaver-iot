package com.milesight.beaveriot.semaphore;

import java.time.Duration;

/**
 * author: Luxb
 * create: 2025/7/25 14:31
 **/
public interface DistributedSemaphore {
    void initPermits(String key, int permits);
    String acquire(String key, Duration timeout);
    void release(String key, String permitId);
}