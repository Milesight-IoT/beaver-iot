package com.milesight.beaveriot.delayedqueue;

/**
 * author: Luxb
 * create: 2025/11/13 10:46
 **/
@FunctionalInterface
public interface DelayedQueueProvider {
    <T> DelayedQueue<T> create(String queueName);
}