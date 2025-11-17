package com.milesight.beaveriot.context.model.delayedqueue;

import java.util.function.Consumer;

/**
 * author: Luxb
 * create: 2025/11/13 9:18
 **/
public interface DelayedQueue<T> {
    void offer(DelayedTask<T> task);
    void cancel(String taskId);
    DelayedTask<T> take() throws InterruptedException;
    DelayedTask<T> poll();
    void addConsumer(Consumer<DelayedTask<T>> consumer);
}