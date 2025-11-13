package com.milesight.beaveriot.delayedqueue;

import com.milesight.beaveriot.delayedqueue.model.DelayedTask;

import java.time.Duration;

/**
 * author: Luxb
 * create: 2025/11/13 9:18
 **/
public interface DelayedQueue<T> {
    void offer(DelayedTask<T> task, Duration delay);
    void cancel(String taskId);
    DelayedTask<T> take();
    DelayedTask<T> poll();
}