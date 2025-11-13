package com.milesight.beaveriot.delayedqueue.local;

import com.milesight.beaveriot.delayedqueue.BaseDelayedQueue;
import com.milesight.beaveriot.delayedqueue.DelayedQueue;
import com.milesight.beaveriot.delayedqueue.model.DelayedTask;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;

/**
 * author: Luxb
 * create: 2025/11/13 9:25
 **/
@Slf4j
public class LocalDelayedQueue<T> extends BaseDelayedQueue implements DelayedQueue<T> {
    private final DelayQueue<DelayedTask<T>> delayQueue;
    private final Map<String, DelayedTask<T>> taskRegistry;

    public LocalDelayedQueue(String queueName) {
        super(queueName);
        delayQueue = new DelayQueue<>();
        taskRegistry = new ConcurrentHashMap<>();
    }

    @Override
    public void offer(DelayedTask<T> task) {
        validateTask(task);

        doWithLock(task.getId(), () -> {
            doCancel(task.getId());
            taskRegistry.put(task.getId(), task);
            delayQueue.offer(task);
            log.debug("Delayed queue {} offered task: {}", queueName, task.getId());
        });
    }

    @Override
    public void cancel(String taskId) {
        if (taskId == null) {
            return;
        }

        doWithLock(taskId, () -> doCancel(taskId));
    }

    private void doCancel(String taskId) {
        if (taskRegistry.containsKey(taskId)) {
            taskRegistry.remove(taskId);
            log.debug("Delayed queue {} cancelled task: {}", queueName, taskId);
        }
    }

    @Override
    public DelayedTask<T> take() {
        while (true) {
            try {
                DelayedTask<T> task = delayQueue.take();

                if (taskRegistry.remove(task.getId(), task)) {
                    log.debug("Delayed queue {} consumed task: {}", queueName, task.getId());
                    return task;
                } else {
                    log.debug("Delayed queue {} skipped stale/cancelled task: {}", queueName, task.getId());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while taking task", e);
                throw new RuntimeException("Thread interrupted", e);
            }
        }
    }

    @Override
    public DelayedTask<T> poll() {
        while (true) {
            DelayedTask<T> task = delayQueue.poll();
            if (task == null) {
                return null;
            }

            if (taskRegistry.remove(task.getId(), task)) {
                log.debug("Delayed queue {} consumed task: {}", queueName, task.getId());
                return task;
            } else {
                log.debug("Delayed queue {} skipped stale/cancelled task: {}", queueName, task.getId());
            }
        }
    }
}