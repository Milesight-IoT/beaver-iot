package com.milesight.beaveriot.delayedqueue.local;

import com.milesight.beaveriot.delayedqueue.DelayedQueueFactory;
import com.milesight.beaveriot.context.model.delayedqueue.DelayedQueue;

/**
 * author: Luxb
 * create: 2025/11/13 10:55
 **/
public class LocalDelayedQueueFactory implements DelayedQueueFactory {
    @Override
    public <T> DelayedQueue<T> create(String queueName) {
        return new LocalDelayedQueue<>(queueName);
    }
}