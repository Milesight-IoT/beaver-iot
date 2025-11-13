package com.milesight.beaveriot.delayedqueue.local;

import com.milesight.beaveriot.delayedqueue.DelayedQueueProvider;
import com.milesight.beaveriot.delayedqueue.DelayedQueue;

/**
 * author: Luxb
 * create: 2025/11/13 10:55
 **/
public class LocalDelayedQueueProvider implements DelayedQueueProvider {
    @Override
    public <T> DelayedQueue<T> create(String queueName) {
        return new LocalDelayedQueue<>(queueName);
    }
}