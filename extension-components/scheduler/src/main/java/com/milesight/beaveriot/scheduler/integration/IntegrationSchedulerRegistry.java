package com.milesight.beaveriot.scheduler.integration;

import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.scheduler.core.Scheduler;
import com.milesight.beaveriot.scheduler.core.model.ScheduleRule;
import com.milesight.beaveriot.scheduler.core.model.ScheduleSettings;
import com.milesight.beaveriot.scheduler.core.model.ScheduleType;
import com.milesight.beaveriot.user.dto.TenantDTO;
import com.milesight.beaveriot.user.facade.IUserFacade;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author loong
 */
@Component
@Slf4j
public class IntegrationSchedulerRegistry {

    private static final Map<String, Runnable> tasks = new ConcurrentHashMap<>();

    private static final Map<String, IntegrationScheduled> integrationSchedules = new ConcurrentHashMap<>();

    private static IntegrationSchedulerRegistry self;

    @Autowired
    private IUserFacade userFacade;

    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    @Autowired
    private Scheduler scheduler;

    @PostConstruct
    public void init() {
        self = this;
    }

    public static void registerScheduler(String schedulerName, Runnable task, IntegrationScheduled scheduled) {
        Assert.isTrue(StringUtils.hasText(schedulerName), "schedulerName cannot be empty");
        tasks.put(schedulerName, task);
        integrationSchedules.put(schedulerName, scheduled);
    }

    public static void updateScheduleAnnotationTask(String schedulerName, List<String> entityKeys) {
        Assert.isTrue(StringUtils.hasText(schedulerName), "schedulerName cannot be empty");
        Runnable task = tasks.get(schedulerName);
        Assert.isTrue(task != null, "task cannot be null");

        IntegrationScheduled integrationScheduled = integrationSchedules.get(schedulerName);
        String cronEntity = integrationScheduled.cronEntity();
        String fixedDelayEntity = integrationScheduled.fixedDelayEntity();
        String timeUnitEntity = integrationScheduled.timeUnitEntity();

        List<TenantDTO> tenants = self.userFacade.getAllTenants();
        for (TenantDTO tenantDTO : tenants) {
            try {
                TenantContext.setTenantId(tenantDTO.getTenantId());

                String cronEntityValue = "";
                long fixedDelayEntityValue = -1;
                String timeUnitEntityValue = "";

                List<String> tenantEntityKeys = new ArrayList<>();
                if (!cronEntity.isEmpty() && entityKeys.contains(cronEntity)) {
                    tenantEntityKeys.add(cronEntity);
                }
                if (!fixedDelayEntity.isEmpty() && entityKeys.contains(fixedDelayEntity)) {
                    tenantEntityKeys.add(fixedDelayEntity);
                }
                if (!timeUnitEntity.isEmpty() && entityKeys.contains(timeUnitEntity)) {
                    tenantEntityKeys.add(timeUnitEntity);
                }
                if (!tenantEntityKeys.isEmpty()) {
                    Map<String, Object> tenantEntityValues = self.entityValueServiceProvider.findValuesByKeys(tenantEntityKeys);
                    if (tenantEntityValues != null && !tenantEntityValues.isEmpty()) {
                        if (!cronEntity.isEmpty() && entityKeys.contains(cronEntity) && tenantEntityValues.containsKey(cronEntity)) {
                            cronEntityValue = tenantEntityValues.get(cronEntity).toString();
                        }
                        if (!fixedDelayEntity.isEmpty() && entityKeys.contains(fixedDelayEntity) && tenantEntityValues.containsKey(fixedDelayEntity)) {
                            fixedDelayEntityValue = Long.parseLong(tenantEntityValues.get(fixedDelayEntity).toString());
                        }
                        if (!timeUnitEntity.isEmpty() && entityKeys.contains(timeUnitEntity) && tenantEntityValues.containsKey(timeUnitEntity)) {
                            timeUnitEntityValue = tenantEntityValues.get(timeUnitEntity).toString();
                        }
                    }
                }
                if (!cronEntityValue.isEmpty() || fixedDelayEntityValue != -1 || !timeUnitEntityValue.isEmpty()) {
                    TimeUnit timeUnit = TimeUnit.MILLISECONDS;
                    if (timeUnitEntityValue != null && !timeUnitEntityValue.isEmpty()) {
                        timeUnit = TimeUnit.valueOf(timeUnitEntityValue);
                    }
                    scheduleTask(schedulerName, tenantDTO, cronEntityValue, fixedDelayEntityValue, timeUnit, task);
                }
            } finally {
                TenantContext.clear();
            }
        }
    }

    public static void scheduleTask(String schedulerName, String cron, Runnable task) {
        List<TenantDTO> tenants = self.userFacade.getAllTenants();
        for (TenantDTO tenantDTO : tenants) {
            scheduleTask(schedulerName, tenantDTO, cron, -1, TimeUnit.MILLISECONDS, task);
        }
    }

    public static void scheduleTask(String schedulerName, long fixedDelay, TimeUnit timeUnit, Runnable task) {
        List<TenantDTO> tenants = self.userFacade.getAllTenants();
        for (TenantDTO tenantDTO : tenants) {
            scheduleTask(schedulerName, tenantDTO, null, fixedDelay, timeUnit, task);
        }
    }

    public static void scheduleTask(String schedulerName) {
        Assert.isTrue(StringUtils.hasText(schedulerName), "schedulerName cannot be empty");
        Runnable task = tasks.get(schedulerName);
        Assert.isTrue(task != null, "task cannot be null");

        IntegrationScheduled integrationScheduled = integrationSchedules.get(schedulerName);
        String cronEntity = integrationScheduled.cronEntity();
        String fixedDelayEntity = integrationScheduled.fixedDelayEntity();
        String timeUnitEntity = integrationScheduled.timeUnitEntity();

        List<TenantDTO> tenants = self.userFacade.getAllTenants();
        for (TenantDTO tenantDTO : tenants) {
            try {
                TenantContext.setTenantId(tenantDTO.getTenantId());

                String cronEntityValue = "";
                long fixedDelayEntityValue = -1;
                String timeUnitEntityValue = "";
                List<String> entityKeys = new ArrayList<>();
                if (!cronEntity.isEmpty()) {
                    entityKeys.add(cronEntity);
                }
                if (!fixedDelayEntity.isEmpty()) {
                    entityKeys.add(fixedDelayEntity);
                }
                if (!timeUnitEntity.isEmpty()) {
                    entityKeys.add(timeUnitEntity);
                }
                if (!entityKeys.isEmpty()) {
                    Map<String, Object> entityValues = self.entityValueServiceProvider.findValuesByKeys(entityKeys);
                    if (entityValues != null && !entityValues.isEmpty()) {
                        if (!cronEntity.isEmpty() && entityValues.containsKey(cronEntity)) {
                            cronEntityValue = entityValues.get(cronEntity).toString();
                        }
                        if (!fixedDelayEntity.isEmpty() && entityValues.containsKey(fixedDelayEntity)) {
                            fixedDelayEntityValue = Long.parseLong(entityValues.get(fixedDelayEntity).toString());
                        }
                        if (!timeUnitEntity.isEmpty() && entityValues.containsKey(timeUnitEntity)) {
                            timeUnitEntityValue = entityValues.get(timeUnitEntity).toString();
                        }
                    }
                }
                String cron = integrationScheduled.cron();
                if (!cronEntityValue.isEmpty()) {
                    cron = cronEntityValue;
                }
                long fixedDelay = integrationScheduled.fixedDelay();
                if (fixedDelayEntityValue >= 0) {
                    fixedDelay = fixedDelayEntityValue;
                }
                TimeUnit timeUnit = integrationScheduled.timeUnit();
                if (!timeUnitEntityValue.isEmpty()) {
                    timeUnit = TimeUnit.valueOf(timeUnitEntityValue);
                }
                scheduleTask(schedulerName, tenantDTO, cron, fixedDelay, timeUnit, task);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private static void scheduleTask(String schedulerName, TenantDTO tenantDTO, String cron, long fixedDelay, TimeUnit timeUnit, Runnable task) {
        Assert.isTrue(StringUtils.hasText(schedulerName), "schedulerName cannot be empty");
        tasks.putIfAbsent(schedulerName, task);

        String tenantId = tenantDTO.getTenantId();
        String taskKey = String.format("integration-scheduler:%s:%s", schedulerName, tenantId);

        Runnable taskWrapper = () -> {
            try {
                TenantContext.setTenantId(tenantId);
                task.run();
            } catch (Exception e) {
                log.error("run task failed: {}", taskKey, e);
            } finally {
                TenantContext.clear();
            }
        };

        if (cron != null && !cron.isEmpty()) {
            String zone = tenantDTO.getTimeZone();
            TimeZone timeZone = zone.isEmpty() ? TimeZone.getDefault() : TimeZone.getTimeZone(zone);
            ZoneId zoneId = timeZone.toZoneId();
            self.scheduler.schedule(taskKey, ScheduleSettings.builder()
                            .scheduleType(ScheduleType.CRON)
                            .scheduleRule(ScheduleRule.builder()
                                    .cronExpressions(Set.of(cron))
                                    .timezone(zoneId.getId())
                                    .build())
                            .build(),
                    scheduledTask -> taskWrapper.run());
        } else if (fixedDelay >= 0) {
            self.scheduler.schedule(taskKey, ScheduleSettings.builder()
                            .scheduleType(ScheduleType.FIXED_RATE)
                            .scheduleRule(ScheduleRule.builder()
                                    .periodSecond(timeUnit.toSeconds(fixedDelay))
                                    .build())
                            .build(),
                    scheduledTask -> taskWrapper.run());
        }
    }

    public static List<IntegrationScheduled> getIntegrationSchedules() {
        if (integrationSchedules.isEmpty()) {
            return null;
        }
        return List.copyOf(integrationSchedules.values());
    }

}
