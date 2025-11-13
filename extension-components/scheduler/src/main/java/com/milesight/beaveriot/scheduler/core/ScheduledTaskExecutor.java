package com.milesight.beaveriot.scheduler.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.pubsub.MessagePubSub;
import com.milesight.beaveriot.pubsub.api.annotation.MessageListener;
import com.milesight.beaveriot.scheduler.core.model.ScheduledTask;
import com.milesight.beaveriot.scheduler.core.model.ScheduledTaskCallbackTerminatedEvent;
import com.milesight.beaveriot.scheduler.core.model.ScheduledTaskCancelledEvent;
import com.milesight.beaveriot.scheduler.core.model.ScheduledTaskRemoteTriggeredEvent;
import com.milesight.beaveriot.scheduler.core.model.ScheduledTaskUpdatedEvent;
import io.netty.util.HashedWheelTimer;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.spring.aop.ScopedLockConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledTaskExecutor {

    public static final int MAX_ATTEMPTS = 3;
    public static final long PRE_FETCH_RANGE = TimeUnit.MINUTES.toSeconds(1);

    public static final long TASK_EXPIRATION = TimeUnit.MINUTES.toSeconds(15);
    public static final String REMOTE_RUN_SCHEDULED_TASK_LOCK_NAME = "REMOTE_RUN_SCHEDULED_TASK(%s,%s)";
    private static final Cache<Long, String> cancelledTaskIds = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .concurrencyLevel(4)
            .build();
    private static final ExecutorService taskExecutor = new ThreadPoolExecutor(1, 20, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    private static final HashedWheelTimer hashedWheelTimer = new HashedWheelTimer(Executors.defaultThreadFactory(), 1000, TimeUnit.MILLISECONDS, 512, true, -1, taskExecutor);
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Autowired
    private ScheduledTaskRepository scheduledTaskRepository;

    @Autowired
    private MessagePubSub messagePubSub;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private LockProvider lockProvider;

    private static long nowEpochSecond() {
        return System.currentTimeMillis() / 1000;
    }

    private static void cancelTasks(String taskKey, List<Long> taskIds) {
        taskIds.forEach(taskId -> cancelledTaskIds.put(taskId, taskKey));
    }

    @Transactional
    @Scheduled(cron = "0 * * * * *")
    @DistributedLock(name = "scheduler", lockAtLeastFor = "59s", lockAtMostFor = "59s", scope = LockScope.GLOBAL, throwOnLockFailure = false)
    public void runTasks() {
        if (!running.get()) {
            log.warn("scheduler is not running");
            return;
        }

        val currentDateTime = ZonedDateTime.now()
                .withSecond(0)
                .withNano(0);
        val currentEpochSecond = currentDateTime.toEpochSecond();
        log.info("run scheduled tasks: {}", currentEpochSecond);

        scheduler.scanScheduledTasksByChunk(0L, currentEpochSecond + PRE_FETCH_RANGE, 500, tasks -> {
            tasks = tasks.stream()
                    // filter out tasks that are running
                    .filter(t -> t.getAttempts() == 0 || t.getExecutionEpochSecond() < currentEpochSecond - PRE_FETCH_RANGE)
                    .toList();

            log.info("{} available tasks found", tasks.size());

            val failedTaskIds = new ArrayList<Long>();
            val runningTaskIds = new ArrayList<Long>();
            tasks.forEach(task -> {
                if (task.getAttempts() >= MAX_ATTEMPTS) {
                    log.info("scheduled task '{}' was failed after {} attempts", task.getId(), task.getAttempts());
                    failedTaskIds.add(task.getId());
                    return;
                }
                runningTaskIds.add(task.getId());
                task.setAttempts(task.getAttempts() + 1);
                runTask(currentDateTime, task);
            });

            if (!failedTaskIds.isEmpty()) {
                markAsFailed(failedTaskIds);
            }
            if (!runningTaskIds.isEmpty()) {
                markAsRunning(runningTaskIds);
            }
        });
    }

    @Transactional
    @Scheduled(cron = "11 11 1 * * *")
    @DistributedLock(name = "scheduled_task_clean_up", lockAtLeastFor = "59s", lockAtMostFor = "59s", scope = LockScope.GLOBAL, throwOnLockFailure = false)
    public void removeExpiredTasks() {
        scheduler.removeExpiredTasks(ZonedDateTime.now().minusMonths(1));
    }


    private void markAsRunning(ArrayList<Long> runningTaskIds) {
        scheduledTaskRepository.increaseAttemptsByIds(runningTaskIds);
    }

    private void markAsFailed(ArrayList<Long> failedTaskIds) {
        scheduledTaskRepository.updateTriggeredAtAndAttemptsByIds(failedTaskIds, nowEpochSecond());
    }

    public void runTask(ZonedDateTime taskExecutionDateTime, ScheduledTask task) {
        val now = nowEpochSecond();
        val delay = task.getExecutionEpochSecond() - now;
        try {
            if (delay <= 1) {
                log.debug("run scheduled task immediately '{}'({})", task.getTaskKey(), task.getId());
                taskExecutor.submit(() -> doRunTask(taskExecutionDateTime, task));
            } else {
                log.debug("scheduled task '{}'({}) will be executed after {} seconds", task.getTaskKey(), task.getId(), delay);
                hashedWheelTimer.newTimeout(timeout -> taskExecutor.submit(() -> doRunTask(taskExecutionDateTime.plusSeconds(delay), task)), delay, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("submit task failed: '{}'", task.getTaskKey(), e);
        }
    }

    private void doRunTask(ZonedDateTime taskExecutionDateTime, ScheduledTask task) {
        if (cancelledTaskIds.getIfPresent(task.getId()) != null) {
            log.info("scheduled task '{}'({}) was cancelled", task.getTaskKey(), task.getId());
            return;
        }

        try {
            val callback = scheduler.getCallback(task.getTaskKey());
            if (callback != null) {
                triggerTaskCallback(task, taskExecutionDateTime, callback);
            } else {
                // the callback may be registered on other nodes
                log.debug("scheduled task callback was not found in local: '{}'", task.getTaskKey());
                messagePubSub.publish(new ScheduledTaskRemoteTriggeredEvent(task, taskExecutionDateTime));
            }
        } catch (Exception e) {
            log.error("execute task '{}' failed", task.getTaskKey(), e);
        }
    }

    private void markAsTriggered(ScheduledTask task) {
        scheduledTaskRepository.updateTriggeredAtByIds(List.of(task.getId()), nowEpochSecond());
    }

    @MessageListener
    public void onRemoteTriggered(ScheduledTaskRemoteTriggeredEvent scheduledTaskRemoteTriggeredEvent) {
        val scheduledTask = scheduledTaskRemoteTriggeredEvent.getScheduledTask();
        val taskExecutionDateTime = scheduledTaskRemoteTriggeredEvent.getTaskExecutionDateTime();
        val callback = scheduler.getCallback(scheduledTask.getTaskKey());
        if (callback == null) {
            log.info("scheduled task callback was not found: '{}'", scheduledTask.getTaskKey());
            return;
        }

        taskExecutor.submit(() -> {
            // ensure only one node can run the task
            val lockConfiguration = ScopedLockConfiguration.builder(LockScope.GLOBAL)
                    .name(REMOTE_RUN_SCHEDULED_TASK_LOCK_NAME.formatted(scheduledTask.getTaskKey(), scheduledTask.getExecutionEpochSecond()))
                    .lockAtLeastFor(Duration.ofSeconds(30))
                    .lockAtMostFor(Duration.ofSeconds(30))
                    .throwOnLockFailure(false)
                    .build();
            lockProvider.lock(lockConfiguration).ifPresentOrElse(lock -> {
                try {
                    triggerTaskCallback(scheduledTask, taskExecutionDateTime, callback);
                } catch (Exception e) {
                    log.error("execute task '{}' failed", scheduledTask.getTaskKey(), e);
                } finally {
                    lock.unlock();
                }
            }, () -> log.info("scheduled task '{}' was triggered already", scheduledTask.getTaskKey()));
        });

    }

    private void triggerTaskCallback(ScheduledTask scheduledTask, ZonedDateTime taskExecutionDateTime, ScheduledTaskCallback callback) {
        val nextExecution = scheduler.createNextTask(scheduledTask, taskExecutionDateTime);
        markAsTriggered(scheduledTask);

        if (taskExecutionDateTime.toEpochSecond() - scheduledTask.getExecutionEpochSecond() > TASK_EXPIRATION) {
            log.info("scheduled task '{}'({}) was expired", scheduledTask.getTaskKey(), scheduledTask.getId());
            return;
        }

        log.debug("scheduled task callback triggered: '{}'", scheduledTask.getTaskKey());

        val originalTenantId = TenantContext.tryGetTenantId().orElse(null);
        try {
            var tenantId = scheduledTask.getTenantId();
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }

            callback.accept(scheduledTask);

            if (nextExecution == null) {
                // unregister callback
                messagePubSub.publishAfterCommit(new ScheduledTaskCallbackTerminatedEvent(List.of(scheduledTask.getTaskKey())));
            }
        } catch (Exception e) {
            log.error("scheduled task '{}' callback failed", scheduledTask.getTaskKey(), e);
        } finally {
            if (originalTenantId != null) {
                TenantContext.setTenantId(originalTenantId);
            } else {
                TenantContext.clear();
            }
        }
    }

    @MessageListener
    public void onScheduledTaskUpdated(ScheduledTaskUpdatedEvent scheduledTaskUpdatedEvent) {
        val scheduledTask = scheduledTaskUpdatedEvent.getScheduledTask();
        if (scheduledTask.getAttempts() > 0) {
            // run task immediately
            runTask(ZonedDateTime.now(), scheduledTask);
        }
        val taskIds = scheduledTaskUpdatedEvent.getPreviousTaskIds();
        if (!CollectionUtils.isEmpty(taskIds)) {
            // cancel previous tasks
            cancelTasks(scheduledTask.getTaskKey(), taskIds);
        }
    }

    @MessageListener
    public void onScheduledTaskCancelled(ScheduledTaskCancelledEvent scheduledTaskCancelledEvent) {
        log.info("remove callback for scheduled task '{}'", scheduledTaskCancelledEvent.getTaskKey());
        scheduler.removeCallback(scheduledTaskCancelledEvent.getTaskKey());
        cancelTasks(scheduledTaskCancelledEvent.getTaskKey(), scheduledTaskCancelledEvent.getTaskIds());
    }

    @MessageListener
    public void onScheduledTaskCallbackTerminated(ScheduledTaskCallbackTerminatedEvent scheduledTaskCallbackTerminatedEvent) {
        log.info("remove callbacks: {}", scheduledTaskCallbackTerminatedEvent.getTaskKeys());
        scheduledTaskCallbackTerminatedEvent.getTaskKeys().forEach(scheduler::removeCallback);
    }

    @PreDestroy
    protected void onDestroy() {
        running.set(false);
        hashedWheelTimer.stop();
        taskExecutor.shutdown();
    }

}
