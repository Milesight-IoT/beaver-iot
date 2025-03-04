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

        IntegrationSchedulerRegistry.scheduleTask(schedulerName);
    }

    @EventSubscribe(payloadKeyExpression = "*")
    public void onScheduleEvent(ExchangeEvent exchangeEvent) {
        List<String> entityKeys = exchangeEvent.getPayload().keySet().stream().toList();
        List<IntegrationScheduled> integrationSchedules = IntegrationSchedulerRegistry.getIntegrationSchedules();
        if (integrationSchedules != null && !integrationSchedules.isEmpty()) {
            integrationSchedules.forEach(integrationScheduled -> {
                IntegrationSchedulerRegistry.updateScheduleAnnotationTask(integrationScheduled.name(), entityKeys);
            });
        }
    }

}
