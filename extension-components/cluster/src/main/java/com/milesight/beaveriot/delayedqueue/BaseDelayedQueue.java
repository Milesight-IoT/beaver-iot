package com.milesight.beaveriot.delayedqueue;

import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.context.model.delayedqueue.DelayedQueue;
import com.milesight.beaveriot.context.model.delayedqueue.DelayedTask;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.context.support.SpringContext;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.spring.aop.ScopedLockConfiguration;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/11/13 13:52
 **/
@Slf4j
public class BaseDelayedQueue<T> implements DelayedQueue<T> {
    private static final LockProvider lockProvider = SpringContext.getBean(LockProvider.class);
    protected final String queueName;
    protected DelayedQueueWrapper<T> delayQueue;
    protected Map<String, Long> taskExpireTimeMap;

    public BaseDelayedQueue(String queueName) {
        String tenantId = TenantContext.tryGetTenantId().orElse("");
        this.queueName = MessageFormat.format(Constants.QUEUE_NAME_FORMAT, tenantId, queueName);
    }

    @Override
    public void offer(DelayedTask<T> task) {
        validateTask(task);

        doWithLock(task.getId(), () -> {
            Long existingExpireTime = taskExpireTimeMap.put(task.getId(), task.renew().getExpireTime());
            delayQueue.offer(task);
            if (existingExpireTime == null) {
                log.debug("Delayed queue {} offered task: {}", queueName, task.getId());
            } else {
                log.debug("Delayed queue {} renewed task: {}", queueName, task.getId());
            }
        });
    }

    @Override
    public void cancel(String taskId) {
        if (taskId == null) {
            return;
        }

        doWithLock(taskId, () -> {
            if (!taskExpireTimeMap.containsKey(taskId)) {
                return;
            }

            taskExpireTimeMap.remove(taskId);
            log.debug("Delayed queue {} cancelled task: {}", queueName, taskId);
        });
    }

    @Override
    public DelayedTask<T> take() {
        while (true) {
            try {
                DelayedTask<T> task = delayQueue.take();

                if (isReallyExpired(task)) {
                    log.debug("Delayed queue {} consumed task: {}", queueName, task.getId());
                    return task;
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

            if (isReallyExpired(task)) {
                log.debug("Delayed queue {} consumed task: {}", queueName, task.getId());
                return task;
            }
        }
    }

    private boolean isReallyExpired(DelayedTask<T> task) {
        return taskExpireTimeMap.remove(task.getId(), task.getExpireTime());
    }

    public void validateTask(DelayedTask<T> task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }

        if (task.getId() == null) {
            throw new IllegalArgumentException("Task id cannot be null");
        }

        if (task.getDelayTime() == null) {
            throw new IllegalArgumentException("Task delay time cannot be null");
        }
    }

    protected void doWithLock(String taskId, Runnable runnable) {
        ScopedLockConfiguration lockConfiguration = ScopedLockConfiguration.builder(LockScope.GLOBAL)
                .name(MessageFormat.format(Constants.LOCK_NAME_DELAYED_QUEUE_HANDLE_TASK_FORMAT, queueName, taskId))
                .lockAtLeastFor(Duration.ofMinutes(0))
                .lockAtMostFor(Duration.ofMinutes(10))
                .throwOnLockFailure(false)
                .build();

        lockProvider.lock(lockConfiguration).ifPresentOrElse(lock -> {
            try {
                runnable.run();
            } finally {
                lock.unlock();
            }
        }, () -> {
            throw new RuntimeException("Another task is running, skipping this task");
        });
    }

    private static class Constants {
        public static final String QUEUE_NAME_FORMAT = "{0}:{1}";
        public static final String LOCK_NAME_DELAYED_QUEUE_HANDLE_TASK_FORMAT = "delayed-queue:{0}:handle-task:{1}";
    }
}