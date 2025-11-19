package com.milesight.beaveriot.scheduler.core;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.google.common.collect.Iterators;
import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.AcquiredLockException;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.pubsub.MessagePubSub;
import com.milesight.beaveriot.scheduler.core.model.ScheduleRule;
import com.milesight.beaveriot.scheduler.core.model.ScheduleSettings;
import com.milesight.beaveriot.scheduler.core.model.ScheduleSettingsPO;
import com.milesight.beaveriot.scheduler.core.model.ScheduleType;
import com.milesight.beaveriot.scheduler.core.model.ScheduledTask;
import com.milesight.beaveriot.scheduler.core.model.ScheduledTaskCallbackTerminatedEvent;
import com.milesight.beaveriot.scheduler.core.model.ScheduledTaskCancelledEvent;
import com.milesight.beaveriot.scheduler.core.model.ScheduledTaskPO;
import com.milesight.beaveriot.scheduler.core.model.ScheduledTaskUpdatedEvent;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class Scheduler {

    private static final Map<String, ScheduledTaskCallback> taskKeyToRunner = new ConcurrentHashMap<>();

    private static final CronParser cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING));

    @Autowired
    private ScheduledTaskRepository scheduledTaskRepository;

    @Autowired
    private ScheduleSettingsRepository scheduleSettingsRepository;

    @Autowired
    private MessagePubSub messagePubSub;

    public void scanScheduledTasksByChunk(Long startEpochSecondInclusive, Long endEpochSecondExclusive, Integer chunkSize,
                                          Consumer<List<ScheduledTask>> chunkConsumer) {
        Pageable pageable = PageRequest.of(0, chunkSize, Sort.by(Sort.Direction.ASC, "executionEpochSecond"));
        while (!pageable.isUnpaged()) {
            log.debug("scan scheduled tasks, chunk number: {}, chunk size: {}, start: {}, end: {}",
                    pageable.getPageNumber(), pageable.getPageSize(), startEpochSecondInclusive, endEpochSecondExclusive);

            val page = scheduledTaskRepository.findAllTasksByExecutionEpochSecondBetween(
                    startEpochSecondInclusive, endEpochSecondExclusive, pageable);
            val taskKeys = page.getContent().stream()
                    .map(ScheduledTaskPO::getTaskKey)
                    .collect(Collectors.toSet());
            val taskKeyToScheduleSetting = scheduleSettingsRepository.findAllByTaskKeyIn(taskKeys)
                    .stream()
                    .collect(Collectors.toMap(ScheduleSettingsPO::getTaskKey, Function.identity(), (v1, v2) -> v1));
            val chunk = page.getContent().stream()
                    .map(po -> buildScheduledTask(po, taskKeyToScheduleSetting.get(po.getTaskKey())))
                    .toList();
            chunkConsumer.accept(chunk);
            pageable = page.nextPageable();
        }
    }

    private ScheduledTask buildScheduledTask(ScheduledTaskPO taskPO, ScheduleSettingsPO scheduleSettingsPO) {
        val scheduledTask = convertToScheduledTask(taskPO);
        scheduledTask.setScheduleSettings(convertToScheduleSettings(scheduleSettingsPO));
        scheduledTask.setTenantId(scheduleSettingsPO.getTenantId());
        return scheduledTask;
    }

    private ScheduledTask convertToScheduledTask(ScheduledTaskPO taskPO) {
        val scheduledTask = new ScheduledTask();
        scheduledTask.setId(taskPO.getId());
        scheduledTask.setTaskKey(taskPO.getTaskKey());
        scheduledTask.setExecutionEpochSecond(taskPO.getExecutionEpochSecond());
        scheduledTask.setAttempts(taskPO.getAttempts());
        scheduledTask.setIteration(taskPO.getIteration());
        scheduledTask.setTriggeredAt(taskPO.getTriggeredAt());
        return scheduledTask;
    }

    private ScheduleSettings convertToScheduleSettings(ScheduleSettingsPO po) {
        val scheduleSettings = new ScheduleSettings();
        val scheduleRule = JsonUtils.fromJSON(po.getScheduleRule(), ScheduleRule.class);
        Objects.requireNonNull(scheduleRule);
        scheduleSettings.setScheduleType(po.getScheduleType());
        scheduleSettings.setScheduleRule(scheduleRule);
        scheduleSettings.setPayload(po.getPayload());
        return scheduleSettings;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @DistributedLock(name = "SCHEDULE(#{#p0})", waitForLock = "3s", scope = LockScope.GLOBAL)
    public ScheduledTask schedule(String taskKey, ScheduleSettings scheduleSettings) throws AcquiredLockException {
        Objects.requireNonNull(taskKey, "taskKey can not be null");
        Objects.requireNonNull(scheduleSettings, "scheduleSettings can not be null");

        List<Long> removedExistingTaskIds = new ArrayList<>();
        var tenantId = TenantContext.tryGetTenantId().orElse(null);
        val existingSchedulePO = scheduleSettingsRepository.findFirstByTaskKey(taskKey);
        if (existingSchedulePO != null) {
            if (existingSchedulePO.getTenantId() == null) {
                // keep null
                tenantId = null;
            } else if (tenantId != null && !Objects.equals(existingSchedulePO.getTenantId(), tenantId)) {
                throw new ServiceException(ErrorCode.FORBIDDEN_PERMISSION, "Tenant '" + tenantId + "' is not allowed to reschedule task '" + taskKey + "'");
            }

            log.info("schedule task '{}' already exists", taskKey);
            val existingSchedule = convertToScheduleSettings(existingSchedulePO);
            val existingTask = getExistingScheduledTask(taskKey, tenantId, existingSchedule, scheduleSettings);
            if (existingTask != null) {
                return existingTask;
            }

            log.info("do reschedule");
            // remove existing tasks
            removedExistingTaskIds = removeSchedule(existingSchedulePO);
        }

        var scheduleSettingsPO = new ScheduleSettingsPO();
        scheduleSettingsPO.setId(SnowflakeUtil.nextId());
        scheduleSettingsPO.setTaskKey(taskKey);
        scheduleSettingsPO.setScheduleType(scheduleSettings.getScheduleType());
        scheduleSettingsPO.setScheduleRule(JsonUtils.toJSON(scheduleSettings.getScheduleRule()));
        scheduleSettingsPO.setPayload(scheduleSettings.getPayload());
        scheduleSettingsPO.setTenantId(tenantId);

        val nowDateTime = ZonedDateTime.now();
        val nextExecutionEpochSecond = getNextExecutionEpochSecond(scheduleSettings.getScheduleType(), scheduleSettings.getScheduleRule(), null, nowDateTime);
        if (nextExecutionEpochSecond == null) {
            throw new IllegalArgumentException("Can not get next execution time, the schedule rule may be invalid");
        }

        var scheduleTaskPO = new ScheduledTaskPO();
        scheduleTaskPO.setId(SnowflakeUtil.nextId());
        scheduleTaskPO.setTaskKey(taskKey);
        scheduleTaskPO.setExecutionEpochSecond(nextExecutionEpochSecond);
        scheduleTaskPO.setTriggeredAt(0L);
        scheduleTaskPO.setIteration(0);

        if (shouldFireNow(nextExecutionEpochSecond, nowDateTime)) {
            scheduleTaskPO.setAttempts(1);
        } else {
            scheduleTaskPO.setAttempts(0);
        }

        scheduleSettingsRepository.save(scheduleSettingsPO);
        scheduleTaskPO = scheduledTaskRepository.save(scheduleTaskPO);

        val scheduledTask = convertToScheduledTask(scheduleTaskPO);
        scheduledTask.setTenantId(tenantId);
        scheduledTask.setScheduleSettings(scheduleSettings);
        messagePubSub.publishAfterCommit(new ScheduledTaskUpdatedEvent(tenantId, scheduledTask, removedExistingTaskIds));

        log.info("schedule task '{}' successfully, next execution time: {}", taskKey, scheduledTask.getExecutionEpochSecond());
        return scheduledTask;
    }

    private static boolean shouldFireNow(Long nextExecutionEpochSecond, ZonedDateTime nowDateTime) {
        return nextExecutionEpochSecond - nowDateTime.toEpochSecond() <= ScheduledTaskExecutor.PRE_FETCH_RANGE;
    }

    @Nullable
    private ScheduledTask getExistingScheduledTask(String taskKey, String tenantId,
                                                   ScheduleSettings existingScheduleSettings, ScheduleSettings newScheduleSettings) {
        if (!newScheduleSettings.equalsWith(existingScheduleSettings)) {
            return null;
        }

        log.info("schedule settings not changed: '{}'", taskKey);
        val taskPO = scheduledTaskRepository.findAllByTaskKey(taskKey)
                .stream()
                .findFirst()
                .orElse(null);
        if (taskPO != null) {
            log.info("existing scheduled task found, execution time: {}", taskPO.getExecutionEpochSecond());
            val task = convertToScheduledTask(taskPO);
            task.setTenantId(tenantId);
            task.setScheduleSettings(existingScheduleSettings);
            return task;
        } else {
            log.warn("existing task not found");
            return null;
        }
    }

    @NonNull
    private List<Long> removeSchedule(ScheduleSettingsPO existingSchedulePO) {
        List<Long> existingTaskIds;
        val existingTasks = scheduledTaskRepository.findAllByTaskKey(existingSchedulePO.getTaskKey());
        existingTaskIds = existingTasks.stream()
                .map(ScheduledTaskPO::getId)
                .toList();
        scheduleSettingsRepository.deleteById(existingSchedulePO.getId());
        if (!existingTaskIds.isEmpty()) {
            scheduledTaskRepository.deleteAllById(existingTaskIds);
        }
        scheduleSettingsRepository.flush();
        return existingTaskIds;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @DistributedLock(name = "SCHEDULE(#{#p0.taskKey})", waitForLock = "3s", scope = LockScope.GLOBAL)
    public ScheduledTask createNextTask(ScheduledTask previousTask, ZonedDateTime currentDateTime) {
        val scheduleSettings = previousTask.getScheduleSettings();
        val scheduleType = scheduleSettings.getScheduleType();
        if (ScheduleType.ONCE.equals(scheduleType)) {
            return null;
        }

        val rule = scheduleSettings.getScheduleRule();
        val previousExecutionDateTime = currentDateTime.toEpochSecond() == previousTask.getExecutionEpochSecond()
                ? currentDateTime
                : ZonedDateTime.ofInstant(Instant.ofEpochSecond(previousTask.getExecutionEpochSecond()), currentDateTime.getZone());
        val nextExecutionEpochSecond = getNextExecutionEpochSecond(scheduleType, rule, previousExecutionDateTime, currentDateTime);
        if (nextExecutionEpochSecond == null) {
            log.info("next execution time not found: '{}'", previousTask.getTaskKey());
            return null;
        }

        var taskPO = scheduledTaskRepository.findFirstByTaskKeyAndExecutionEpochSecond(previousTask.getTaskKey(), nextExecutionEpochSecond);
        boolean taskExists = taskPO != null;
        if (taskExists) {
            log.debug("schedule task '{}' next execution {} already exists", previousTask.getTaskKey(), nextExecutionEpochSecond);
        } else {
            log.debug("schedule task '{}' create next execution: {}", previousTask.getTaskKey(), nextExecutionEpochSecond);
            taskPO = scheduledTaskRepository.save(ScheduledTaskPO.builder()
                    .id(SnowflakeUtil.nextId())
                    .taskKey(previousTask.getTaskKey())
                    .executionEpochSecond(nextExecutionEpochSecond)
                    .attempts(shouldFireNow(nextExecutionEpochSecond, currentDateTime) ? 1 : 0)
                    .iteration(previousTask.getIteration() + 1)
                    .triggeredAt(0L)
                    .build());
        }

        val task = convertToScheduledTask(taskPO);
        task.setScheduleSettings(scheduleSettings);
        task.setTenantId(previousTask.getTenantId());

        if (!taskExists && taskPO.getAttempts() > 0) {
            messagePubSub.publishAfterCommit(new ScheduledTaskUpdatedEvent(task.getTenantId(), task, Collections.emptyList()));
        }
        return task;
    }

    public Long getNextExecutionEpochSecond(ScheduleType scheduleType, ScheduleRule scheduleRule, ZonedDateTime previousExecutionDateTime, ZonedDateTime currentDateTime) {
        if (scheduleType == null || scheduleRule == null) {
            return null;
        }

        return switch (scheduleType) {
            case ONCE -> getNextExecutionEpochSecondByOnce(scheduleRule, previousExecutionDateTime, currentDateTime);
            case CRON -> getNextExecutionEpochSecondByCron(scheduleRule, previousExecutionDateTime, currentDateTime);
            case FIXED_RATE ->
                    getNextExecutionEpochSecondByFixedRate(scheduleRule, previousExecutionDateTime, currentDateTime);
        };
    }

    @Nullable
    private static Long getNextExecutionEpochSecondByOnce(ScheduleRule scheduleRule, ZonedDateTime previousExecutionDateTime, ZonedDateTime currentDateTime) {
        if (previousExecutionDateTime != null) {
            return null;
        }
        val currentEpochSecond = currentDateTime.toEpochSecond();
        val startEpochSecond = scheduleRule.getStartEpochSecond();
        if (startEpochSecond == null) {
            return null;
        }
        if (startEpochSecond > currentEpochSecond) {
            return startEpochSecond;
        }
        return null;
    }

    @Nullable
    private static Long getNextExecutionEpochSecondByCron(ScheduleRule scheduleRule, ZonedDateTime previousExecutionDateTime, ZonedDateTime currentDateTime) {
        val expirationEpochSecond = scheduleRule.getExpirationEpochSecond();
        val startEpochSecond = scheduleRule.getStartEpochSecond();
        currentDateTime = previousExecutionDateTime == null || currentDateTime.isAfter(previousExecutionDateTime)
                ? currentDateTime
                : previousExecutionDateTime;

        if (scheduleRule.getTimezone() != null) {
            val zoneId = ZoneId.of(scheduleRule.getTimezone());
            if (zoneId != currentDateTime.getZone()) {
                currentDateTime = currentDateTime.withZoneSameInstant(zoneId);
            }
        }

        if (startEpochSecond != null && startEpochSecond > currentDateTime.toEpochSecond()) {
            currentDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(startEpochSecond), currentDateTime.getZone());
        }
        val startDateTime = currentDateTime;

        return scheduleRule.getCronExpressions().stream()
                .map(cronExpression -> {
                    val cron = cronParser.parse(cronExpression);
                    val executionTime = ExecutionTime.forCron(cron);
                    return executionTime.nextExecution(startDateTime)
                            .map(ZonedDateTime::toEpochSecond)
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .sorted()
                .findFirst()
                .filter(cronNext -> expirationEpochSecond == null || cronNext <= expirationEpochSecond)
                .orElse(null);
    }

    @Nullable
    private static Long getNextExecutionEpochSecondByFixedRate(ScheduleRule scheduleRule, ZonedDateTime previousExecutionDateTime, ZonedDateTime currentDateTime) {
        val currentEpochSecond = currentDateTime.toEpochSecond();
        val expirationEpochSecond = scheduleRule.getExpirationEpochSecond();
        val periodSecond = scheduleRule.getPeriodSecond();
        val startEpochSecond = scheduleRule.getStartEpochSecond();

        if (previousExecutionDateTime == null && startEpochSecond != null && startEpochSecond > currentEpochSecond) {
            return startEpochSecond;
        } else {
            if (periodSecond == null || periodSecond <= 0) {
                return null;
            }
            if (previousExecutionDateTime == null) {
                previousExecutionDateTime = currentDateTime;
            }
            val previousExecutionEpochSecond = previousExecutionDateTime.toEpochSecond();
            long fixedRateNext = previousExecutionEpochSecond + periodSecond;
            // ensure next execution is after current time
            while (fixedRateNext < currentEpochSecond) {
                fixedRateNext = fixedRateNext + periodSecond;
            }
            if (expirationEpochSecond == null || expirationEpochSecond >= fixedRateNext) {
                return fixedRateNext;
            }
        }
        return null;
    }

    public ScheduledTask schedule(String taskKey, ScheduleSettings scheduleSettings, ScheduledTaskCallback callback) {
        ScheduledTask result = null;
        try {
            result = self().schedule(taskKey, scheduleSettings);
        } catch (AcquiredLockException e) {
            log.warn("schedule task '{}' conflict", taskKey, e);
            // other nodes scheduled the task, continue
        }
        registerCallback(taskKey, callback);
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @DistributedLock(name = "SCHEDULE(#{#p0})", waitForLock = "3s", scope = LockScope.GLOBAL)
    public void cancel(String taskKey) {
        log.info("cancel schedule task: '{}'", taskKey);
        val scheduleSettings = scheduleSettingsRepository.findFirstByTaskKey(taskKey);
        if (scheduleSettings != null) {
            scheduleSettingsRepository.deleteById(scheduleSettings.getId());
        }

        val tasks = scheduledTaskRepository.findAllByTaskKey(taskKey);
        val taskIds = tasks.stream()
                .map(ScheduledTaskPO::getId)
                .toList();
        if (!taskIds.isEmpty()) {
            scheduledTaskRepository.deleteAllById(taskIds);
            messagePubSub.publishAfterCommit(new ScheduledTaskCancelledEvent(taskKey, taskIds));
        }

        removeCallback(taskKey);
    }

    public void registerCallback(String taskKey, ScheduledTaskCallback callback) {
        var previous = taskKeyToRunner.put(taskKey, callback);
        if (previous != null) {
            log.info("callback for schedule task '{}' was updated", taskKey);
        } else {
            log.info("callback for schedule task '{}' was registered", taskKey);
        }
    }

    public ScheduledTaskCallback getCallback(String taskKey) {
        return taskKeyToRunner.get(taskKey);
    }

    public void removeCallback(String taskKey) {
        log.info("remove callback for schedule task '{}'", taskKey);
        taskKeyToRunner.remove(taskKey);
    }

    @Transactional
    public void removeExpiredTasks(ZonedDateTime expirationDateTime) {
        log.info("remove expired schedule tasks before {}", expirationDateTime);
        scheduledTaskRepository.deleteAllExpired(expirationDateTime.toEpochSecond());
        val taskKeys = scheduleSettingsRepository.findTerminatedTaskKeys();
        if (!taskKeys.isEmpty()) {
            Iterators.partition(taskKeys.iterator(), 200).forEachRemaining(keys -> {
                log.info("remove terminated schedule tasks: {}", keys);
                scheduleSettingsRepository.deleteAllByTaskKeyIn(keys);
                messagePubSub.publishAfterCommit(new ScheduledTaskCallbackTerminatedEvent(keys));
            });
        }
    }

    public Scheduler self() {
        return (Scheduler) AopContext.currentProxy();
    }

}
