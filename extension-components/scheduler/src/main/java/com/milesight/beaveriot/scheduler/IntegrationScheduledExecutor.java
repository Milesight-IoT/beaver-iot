package com.milesight.beaveriot.scheduler;

import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author loong
 */
@Component
@Slf4j
public class IntegrationScheduledExecutor implements SmartInitializingSingleton {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    @Override
    public void afterSingletonsInstantiated() {
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class);

        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Method[] methods = ReflectionUtils.getAllDeclaredMethods(bean.getClass());

            for (Method method : methods) {
                IntegrationScheduled scheduled = method.getAnnotation(IntegrationScheduled.class);
                if (scheduled != null && scheduled.enabled()) {
                    configureTask(bean, method, scheduled);
                }
            }
        }
    }

    private void configureTask(Object bean, Method method, IntegrationScheduled scheduled) {
        Runnable task = TaskUtils.decorateTaskWithErrorHandler(() -> {
            try {
                method.invoke(bean);
            } catch (Exception e) {
                log.error("IntegrationScheduled method invoke error, method: {}", method, e);
                throw new RuntimeException(e);
            }
        }, null, false);
        String schedulerName = scheduled.name();
        IntegrationSchedulerRegistry.registerScheduler(schedulerName, task, scheduled);

        String cronEntity = scheduled.cronEntity();
        String fixedDelayEntity = scheduled.fixedDelayEntity();
        String timeUnitEntity = scheduled.timeUnitEntity();

        String cronEntityValue = "";
        if (!cronEntity.isEmpty()) {
            cronEntityValue = entityValueServiceProvider.findValueByKey(cronEntity).toString();
        }
        long fixedDelayEntityValue = -1;
        if (!fixedDelayEntity.isEmpty()) {
            fixedDelayEntityValue = Long.parseLong(entityValueServiceProvider.findValueByKey(fixedDelayEntity).toString());
        }
        String timeUnitEntityValue = "";
        if (!timeUnitEntity.isEmpty()) {
            timeUnitEntityValue = entityValueServiceProvider.findValueByKey(timeUnitEntity).toString();
        }
        String cron = scheduled.cron();
        if (!cronEntityValue.isEmpty()) {
            cron = cronEntityValue;
        }
        long fixedDelay = scheduled.fixedDelay();
        if (fixedDelayEntityValue >= 0) {
            fixedDelay = fixedDelayEntityValue;
        }
        TimeUnit timeUnit = scheduled.timeUnit();
        if (!timeUnitEntityValue.isEmpty()) {
            timeUnit = TimeUnit.valueOf(timeUnitEntityValue);
        }
        IntegrationSchedulerRegistry.scheduleTask(schedulerName, cron, fixedDelay, timeUnit, task);
    }

    @EventSubscribe(payloadKeyExpression = "*")
    public void onScheduleEvent(ExchangeEvent exchangeEvent) {
        List<String> entityKeys = exchangeEvent.getPayload().keySet().stream().toList();
        List<IntegrationScheduled> integrationSchedules = IntegrationSchedulerRegistry.getIntegrationSchedules();
        if (integrationSchedules != null && !integrationSchedules.isEmpty()) {
            integrationSchedules.forEach(integrationScheduled -> {
                String cronEntity = integrationScheduled.cronEntity();
                String fixedDelayEntity = integrationScheduled.fixedDelayEntity();
                String timeUnitEntity = integrationScheduled.timeUnitEntity();

                String cronEntityValue = "";
                long fixedDelayEntityValue = -1;
                String timeUnitEntityValue = "";
                if (!cronEntity.isEmpty()) {
                    if (entityKeys.contains(cronEntity)) {
                        cronEntityValue = entityValueServiceProvider.findValueByKey(cronEntity).toString();
                    }
                }
                if (!fixedDelayEntity.isEmpty()) {
                    if (entityKeys.contains(fixedDelayEntity)) {
                        fixedDelayEntityValue = Long.parseLong(entityValueServiceProvider.findValueByKey(fixedDelayEntity).toString());
                    }
                }
                if (!timeUnitEntity.isEmpty()) {
                    if (entityKeys.contains(timeUnitEntity)) {
                        timeUnitEntityValue = entityValueServiceProvider.findValueByKey(timeUnitEntity).toString();
                    }
                }
                if (!cronEntityValue.isEmpty() || fixedDelayEntityValue != -1 || !timeUnitEntityValue.isEmpty()) {
                    IntegrationSchedulerRegistry.updateScheduleAnnotationTask(integrationScheduled.name(), cronEntityValue, fixedDelayEntityValue, timeUnitEntityValue);
                }
            });
        }
    }

}
