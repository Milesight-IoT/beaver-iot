package com.milesight.beaveriot.delayedqueue;

import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.context.model.delayedqueue.DelayedQueue;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.context.model.delayedqueue.DelayedTask;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.spring.aop.ScopedLockConfiguration;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
        this.queueName = MessageFormat.format("{0}:{1}", tenantId, queueName);
    }

    @Override
    public void offer(DelayedTask<T> task) {
        validateTask(task);

        doWithLock(task.getId(), () -> {
            task.renew();
            boolean isTaskExist = taskExpireTimeMap.containsKey(task.getId());
            taskExpireTimeMap.put(task.getId(), task.getExpireTime());
            delayQueue.offer(task);
            if (isTaskExist) {
                log.debug("Delayed queue {} renewed task: {}", queueName, task.getId());
            } else {
                log.debug("Delayed queue {} offered task: {}", queueName, task.getId());
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
        AtomicBoolean isReallyExpired = new AtomicBoolean(false);
        doWithLock(task.getId(), () -> {
            Long expireTime = taskExpireTimeMap.get(task.getId());
            if (expireTime == null) {
                return;
            }

            if (expireTime <= System.currentTimeMillis()) {
                isReallyExpired.set(true);
                taskExpireTimeMap.remove(task.getId());
            }
        });
        return isReallyExpired.get();
    }

    public void validateTask(DelayedTask<T> task) {
        if (task == null || task.getId() == null) {
            throw new IllegalArgumentException("Task or taskId cannot be null");
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
        public static final String LOCK_NAME_DELAYED_QUEUE_HANDLE_TASK_FORMAT = "delayed-queue:{0}:handle-task:{1}";
    }
}