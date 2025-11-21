package com.milesight.beaveriot.delayedqueue;

import com.milesight.beaveriot.context.model.delayedqueue.DelayedTask;

/**
 * author: Luxb
 * create: 2025/11/14 10:34
 **/
public interface DelayedQueueWrapper<T> {
    void offer(DelayedTask<T> task);
    DelayedTask<T> take() throws InterruptedException;
    boolean isEmpty();
}
