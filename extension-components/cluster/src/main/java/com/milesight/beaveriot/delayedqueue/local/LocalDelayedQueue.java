package com.milesight.beaveriot.delayedqueue.local;

import com.milesight.beaveriot.delayedqueue.BaseDelayedQueue;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * author: Luxb
 * create: 2025/11/13 9:25
 **/
@Slf4j
public class LocalDelayedQueue<T> extends BaseDelayedQueue<T> {
    public LocalDelayedQueue(String queueName) {
        super(queueName, new LocalDelayedQueueWrapper<>(), new ConcurrentHashMap<>());
    }
}