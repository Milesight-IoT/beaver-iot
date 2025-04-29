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
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author loong
 */
@Component
@Slf4j
public class IntegrationSchedulerRegistry {

    private static final Map<String, Runnable> tasks = new ConcurrentHashMap<>();

    private static final Map<String, IntegrationScheduled> integrationSchedules = new ConcurrentHashMap<>();
    private static final String SCHEDULED_TASK_PREFIX = "integration-scheduler";

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
        Assert.hasText(schedulerName, "schedulerName cannot be empty");
        tasks.put(schedulerName, task);
        IntegrationScheduled replaced = integrationSchedules.put(schedulerName, scheduled);
        if (replaced != null) {
            log.info("integration scheduler '{}' has been registered already, now replace it", schedulerName);
        }
    }

    public static void scheduleTask(IntegrationScheduled integrationScheduled, Set<String> entityKeys) {
        String tenantId = TenantContext.getTenantId();
        TenantDTO tenant = self.userFacade.analyzeTenantId(tenantId);
        Assert.notNull(tenant, "tenant not found");
        scheduleTask(integrationScheduled, tenant, entityKeys);
    }

    public static void scheduleTask(IntegrationScheduled integrationScheduled) {
        scheduleTask(integrationScheduled, null, null);
    }

    public static void scheduleTask(IntegrationScheduled integrationScheduled, TenantDTO tenant) {
        scheduleTask(integrationScheduled, tenant, null);
    }


    public static void scheduleTask(IntegrationScheduled integrationScheduled, TenantDTO tenant, Set<String> updatedEntityKeys) {
        Optional<String> previousTenantId = TenantContext.tryGetTenantId();
        try {
            String tenantId = null;
            if (tenant == null) {
                // global task
                TenantContext.clear();
            } else {
                tenantId = tenant.getTenantId();
                TenantContext.setTenantId(tenantId);
            }

            Assert.hasText(integrationScheduled.name(), "schedulerName cannot be empty");
            Runnable task = tasks.get(integrationScheduled.name());
            Assert.notNull(task, "task not found");

            String timeZoneEntityKey = integrationScheduled.timeZoneEntity();
            String cronEntityKey = integrationScheduled.cronEntity();
            String fixedRateEntityKey = integrationScheduled.fixedRateEntity();
            String timeUnitEntityKey = integrationScheduled.timeUnitEntity();
            String enabledEntityKey = integrationScheduled.enabledEntity();

            SchedulerSettings settings = new SchedulerSettings(
                    integrationScheduled.enabled(),
                    integrationScheduled.timeZone(),
                    integrationScheduled.cron(),
                    integrationScheduled.fixedRate(),
                    integrationScheduled.timeUnit());

            List<String> definedSchedulerEntityKeys = Stream.of(timeZoneEntityKey, cronEntityKey, fixedRateEntityKey,
                            timeUnitEntityKey, enabledEntityKey)
                    .filter(s -> !s.isEmpty())
                    .toList();

            if (updatedEntityKeys != null) {
                boolean isMatch = definedSchedulerEntityKeys.stream().anyMatch(updatedEntityKeys::contains);
                if (!isMatch) {
                    log.debug("given entity key set does not contain any defined scheduler entity keys, skip. scheduler name: '{}', tenant: '{}'", integrationScheduled.name(), tenantId);
                    return;
                }
            }

            Map<String, Object> schedulerEntityValues = self.entityValueServiceProvider.findValuesByKeys(definedSchedulerEntityKeys);
            handleMapValueIfExists(schedulerEntityValues, timeZoneEntityKey, v -> settings.timeZone = v.toString());
            handleMapValueIfExists(schedulerEntityValues, cronEntityKey, v -> settings.cron = v.toString());
            handleMapValueIfExists(schedulerEntityValues, fixedRateEntityKey, v -> settings.fixedRate = Long.parseLong(v.toString()));
            handleMapValueIfExists(schedulerEntityValues, timeUnitEntityKey, v -> settings.timeUnit = parseTimeUnit(v, settings.timeUnit));
            handleMapValueIfExists(schedulerEntityValues, enabledEntityKey, v -> settings.enabled = !Boolean.FALSE.equals(v));

            if (!settings.enabled) {
                log.debug("integration scheduler is disabled, cancel task. scheduler name: '{}', tenant: '{}'", integrationScheduled.name(), tenantId);
                self.scheduler.cancel(getTaskKey(integrationScheduled.name(), tenantId));
                return;
            }

            if (!StringUtils.hasText(settings.timeZone)) {
                settings.timeZone = tenant != null ? tenant.getTimeZone() : TimeZone.getDefault().getID();
            }

            if (StringUtils.hasText(settings.cron) || settings.fixedRate > 0) {
                doScheduleTask(integrationScheduled.name(), tenantId, settings, task);
            } else {
                log.debug("integration scheduler configuration is invalid, skip. scheduler name: '{}', tenant: '{}'", integrationScheduled.name(), tenantId);
            }
        } finally {
            previousTenantId.ifPresentOrElse(TenantContext::setTenantId, TenantContext::clear);
        }
    }

    @Nullable
    private static TimeUnit parseTimeUnit(Object v, TimeUnit defaultValue) {
        TimeUnit timeUnit = defaultValue;
        try {
            timeUnit = TimeUnit.valueOf(v.toString());
        } catch (IllegalArgumentException e) {
            log.error("invalid time unit '{}'", v);
        }
        return timeUnit;
    }

    private static void handleMapValueIfExists(Map<String, Object> map, String key, Consumer<Object> setter) {
        if (map == null || map.isEmpty() || key == null || key.isEmpty()) {
            return;
        }
        Object value = map.get(key);
        if (value != null) {
            setter.accept(value);
        }
    }

    public static void scheduleTask(IntegrationScheduled integrationScheduled, List<TenantDTO> tenants) {
        if (isGlobal(integrationScheduled)) {
            scheduleTask(integrationScheduled);
        } else {
            for (TenantDTO tenant : tenants) {
                scheduleTask(integrationScheduled, tenant);
            }
        }
    }

    public static List<TenantDTO> getAllTenants() {
        return self.userFacade.getAllTenants();
    }

    private static void doScheduleTask(String schedulerName, String tenantId, SchedulerSettings settings, Runnable task) {
        Assert.hasText(schedulerName, "schedulerName cannot be empty");

        String taskKey = getTaskKey(schedulerName, tenantId);
        Runnable taskWrapper = () -> {
            try {
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                } else {
                    TenantContext.clear();
                }
                task.run();
            } catch (Exception e) {
                log.error("run task failed: {}", taskKey, e);
            } finally {
                TenantContext.clear();
            }
        };

        if (StringUtils.hasText(settings.cron)) {
            TimeZone tz = StringUtils.hasText(settings.timeZone) ? TimeZone.getTimeZone(settings.timeZone) : TimeZone.getDefault();
            ZoneId zoneId = tz.toZoneId();
            self.scheduler.schedule(taskKey, ScheduleSettings.builder()
                            .scheduleType(ScheduleType.CRON)
                            .scheduleRule(ScheduleRule.builder()
                                    .cronExpressions(Set.of(settings.cron))
                                    .timezone(zoneId.getId())
                                    .build())
                            .build(),
                    scheduledTask -> taskWrapper.run());
        } else if (settings.fixedRate > 0) {
            self.scheduler.schedule(taskKey, ScheduleSettings.builder()
                            .scheduleType(ScheduleType.FIXED_RATE)
                            .scheduleRule(ScheduleRule.builder()
                                    .periodSecond(settings.timeUnit.toSeconds(settings.fixedRate))
                                    .build())
                            .build(),
                    scheduledTask -> taskWrapper.run());
        }
    }

    private static String getTaskKey(String schedulerName, String tenantId) {
        if (tenantId == null) {
            return String.format("%s:%s", SCHEDULED_TASK_PREFIX, schedulerName);
        }
        return String.format("%s:%s:%s", SCHEDULED_TASK_PREFIX, schedulerName, tenantId);
    }

    public static List<IntegrationScheduled> getIntegrationSchedules() {
        if (integrationSchedules.isEmpty()) {
            return null;
        }
        return List.copyOf(integrationSchedules.values());
    }

    public static boolean isGlobal(IntegrationScheduled integrationScheduled) {
        String cronEntityKey = integrationScheduled.cronEntity();
        String fixedRateEntityKey = integrationScheduled.fixedRateEntity();
        String timeUnitEntityKey = integrationScheduled.timeUnitEntity();
        String enabledEntityKey = integrationScheduled.enabledEntity();
        return !StringUtils.hasText(cronEntityKey)
                && !StringUtils.hasText(fixedRateEntityKey)
                && !StringUtils.hasText(timeUnitEntityKey)
                && !StringUtils.hasText(enabledEntityKey);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @SuppressWarnings({"java:S6548"})
    private static final class SchedulerSettings {
        private boolean enabled = true;
        private String timeZone;
        private String cron;
        private long fixedRate = -1;
        private TimeUnit timeUnit = TimeUnit.SECONDS;
    }

}
