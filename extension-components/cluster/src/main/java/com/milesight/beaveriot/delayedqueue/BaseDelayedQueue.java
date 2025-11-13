package com.milesight.beaveriot.delayedqueue;

import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.delayedqueue.model.DelayedTask;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.spring.aop.ScopedLockConfiguration;

import java.text.MessageFormat;
import java.time.Duration;

/**
 * author: Luxb
 * create: 2025/11/13 13:52
 **/
@Slf4j
public class BaseDelayedQueue {
    private static final LockProvider lockProvider = SpringContext.getBean(LockProvider.class);
    protected final String queueName;

    public BaseDelayedQueue(String queueName) {
        String tenantId = TenantContext.tryGetTenantId().orElse("");
        this.queueName = MessageFormat.format("{0}:{1}", tenantId, queueName);
    }

    public <T> void validateTask(DelayedTask<T> task) {
        if (task == null || task.getId() == null) {
            throw new IllegalArgumentException("Task or taskId cannot be null");
        }
    }

    protected void doWithLock(String taskId, Runnable runnable) {
        ScopedLockConfiguration lockConfiguration = ScopedLockConfiguration.builder(LockScope.TENANT)
                .name(MessageFormat.format("delayed-queue:{0}:handle-task:{1}", queueName, taskId))
                .lockAtLeastFor(Duration.ofMinutes(0))
                .lockAtMostFor(Duration.ofMinutes(5))
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
}