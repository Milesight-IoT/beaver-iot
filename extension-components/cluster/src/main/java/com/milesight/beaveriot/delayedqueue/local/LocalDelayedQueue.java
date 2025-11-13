package com.milesight.beaveriot.delayedqueue.local;

import com.milesight.beaveriot.delayedqueue.DelayedQueue;
import com.milesight.beaveriot.delayedqueue.model.DelayedTask;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * author: Luxb
 * create: 2025/11/13 9:25
 **/
@Slf4j
public class LocalDelayedQueue<T> implements DelayedQueue<T> {
    private final String queueName;

    public LocalDelayedQueue(String queueName) {
        this.queueName = queueName;
    }

    @Override
    public void offer(DelayedTask<T> task, Duration delay) {
        log.info("offer from local: taskId:{}", task.getTaskId());
    }

    @Override
    public void cancel(String taskId) {

    }

    @Override
    public DelayedTask<T> take() {
        return null;
    }

    @Override
    public DelayedTask<T> poll() {
        return null;
    }
}