package com.milesight.beaveriot.delayedqueue;

import com.milesight.beaveriot.context.model.delayedqueue.DelayedQueue;

/**
 * author: Luxb
 * create: 2025/11/13 10:46
 **/
@FunctionalInterface
public interface DelayedQueueFactory {
    <T> DelayedQueue<T> create(String queueName);
}