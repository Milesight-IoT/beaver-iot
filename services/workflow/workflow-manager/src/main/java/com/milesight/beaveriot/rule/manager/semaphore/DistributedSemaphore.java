package com.milesight.beaveriot.rule.manager.semaphore;

import java.time.Duration;

/**
 * author: Luxb
 * create: 2025/7/25 14:31
 **/
public interface DistributedSemaphore {
    void initPermits(String key, int permits);
    boolean acquire(String key, Duration timeout);
    void release(String key);
}