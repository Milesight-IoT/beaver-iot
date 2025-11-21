package com.milesight.beaveriot.delayedqueue.local;

import com.milesight.beaveriot.context.model.delayedqueue.DelayedTask;
import com.milesight.beaveriot.delayedqueue.DelayedQueueWrapper;

import java.util.concurrent.DelayQueue;

/**
 * author: Luxb
 * create: 2025/11/14 10:38
 **/
public class LocalDelayedQueueWrapper<T> implements DelayedQueueWrapper<T> {
    private final DelayQueue<DelayedTask<T>> innerDelayedQueue;

    public LocalDelayedQueueWrapper() {
        this.innerDelayedQueue = new DelayQueue<>();
    }

    @Override
    public void offer(DelayedTask<T> task) {
        innerDelayedQueue.offer(task);
    }

    @Override
    public DelayedTask<T> take() throws InterruptedException {
        return innerDelayedQueue.take();
    }

    @Override
    public boolean isEmpty() {
        return innerDelayedQueue.isEmpty();
    }
}
