package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.model.delayedqueue.DelayedQueue;

/**
 * author: Luxb
 * create: 2025/11/14 13:38
 **/
public interface DelayedQueueServiceProvider {
    <T> DelayedQueue<T> getDelayedQueue(String queueName);
}
