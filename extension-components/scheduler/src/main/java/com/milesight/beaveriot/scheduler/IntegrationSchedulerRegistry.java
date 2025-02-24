package com.milesight.beaveriot.scheduler;

import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.user.dto.TenantDTO;
import com.milesight.beaveriot.user.facade.IUserFacade;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author loong
 */
@Component
@Slf4j
public class IntegrationSchedulerRegistry {

    private static final Map<String, Runnable> tasks = new ConcurrentHashMap<>();
    private static final Map<String, IntegrationScheduled> integrationSchedules = new ConcurrentHashMap<>();
    private static final Map<String, ScheduledFuture<?>> taskFutures = new ConcurrentHashMap<>();

    private static List<TenantDTO> tenants = new ArrayList<>();

    @Autowired
    IUserFacade userFacade;

    @PostConstruct
    public void init() {
        tenants = userFacade.getAllTenants();
    }

    public static void registerScheduler(String schedulerName, Runnable task, IntegrationScheduled scheduled) {
        Assert.isTrue(StringUtils.hasText(schedulerName), "schedulerName cannot be empty");
        tasks.put(schedulerName, task);
        integrationSchedules.put(schedulerName, scheduled);
    }

    public static void updateScheduleAnnotationTask(String schedulerName, String cronEntityValue, long fixedDelayEntityValue, String timeUnitEntityValue) {
        Assert.isTrue(StringUtils.hasText(schedulerName), "schedulerName cannot be empty");
        Runnable task = tasks.get(schedulerName);
        Assert.isTrue(task != null, "task cannot be null");

        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        if (timeUnitEntityValue != null && !timeUnitEntityValue.isEmpty()) {
            timeUnit = TimeUnit.valueOf(timeUnitEntityValue);
        }
        scheduleTask(schedulerName, cronEntityValue, fixedDelayEntityValue, timeUnit, task);
    }

    private static void cancelTask(String taskFutureKey) {
        ScheduledFuture<?> future = taskFutures.get(taskFutureKey);
        if (future != null) {
            future.cancel(true);
            taskFutures.remove(taskFutureKey);
        }
    }

    public static void scheduleTask(String schedulerName, String cron, Runnable task) {
        scheduleTask(schedulerName, cron, -1, TimeUnit.MILLISECONDS, task);
    }

    public static void scheduleTask(String schedulerName, long fixedDelay, TimeUnit timeUnit, Runnable task) {
        scheduleTask(schedulerName, null, fixedDelay, timeUnit, task);
    }

    public static void scheduleTask(String schedulerName, String cron, long fixedDelay, TimeUnit timeUnit, Runnable task) {
        Assert.isTrue(StringUtils.hasText(schedulerName), "schedulerName cannot be empty");
        tasks.putIfAbsent(schedulerName, task);

        for (TenantDTO tenant : tenants) {
            String tenantId = tenant.getTenantId();
            String taskFutureKey = schedulerName + "_" + tenantId;

            cancelTask(taskFutureKey);

            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(1);
            scheduler.setThreadNamePrefix("integration-scheduled-task-" + taskFutureKey + "-");
            scheduler.initialize();

            Runnable taskWrapper = () -> {
                try {
                    TenantContext.setTenantId(tenantId);
                    task.run();
                } catch (Exception e) {
                    log.error("run task failed: {}", taskFutureKey, e);
                } finally {
                    TenantContext.clear();
                }
            };

            if (cron != null && !cron.isEmpty()) {
                String zone = tenant.getTimeZone();
                TimeZone timeZone = zone.isEmpty() ? TimeZone.getDefault() : TimeZone.getTimeZone(zone);
                ScheduledFuture<?> future = scheduler.schedule(taskWrapper, new CronTrigger(cron, timeZone));
                taskFutures.put(taskFutureKey, future);
            } else if (fixedDelay >= 0) {
                PeriodicTrigger trigger = new PeriodicTrigger(timeUnit.toMillis(fixedDelay));
                ScheduledFuture<?> future = scheduler.schedule(taskWrapper, trigger);
                taskFutures.put(taskFutureKey, future);
            }
        }
    }

    public static List<IntegrationScheduled> getIntegrationSchedules() {
        if (integrationSchedules.isEmpty()) {
            return null;
        }
        return List.copyOf(integrationSchedules.values());
    }

}
