package com.milesight.beaveriot.scheduler.integration;

import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.user.dto.TenantDTO;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        List<TenantDTO> tenants = IntegrationSchedulerRegistry.getAllTenants();

        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Method[] methods = ReflectionUtils.getAllDeclaredMethods(bean.getClass());

            for (Method method : methods) {
                IntegrationScheduled scheduled = method.getAnnotation(IntegrationScheduled.class);
                if (scheduled != null) {
                    configureTask(tenants, bean, method, scheduled);
                }
            }
        }
    }

    private void configureTask(List<TenantDTO> tenants, Object bean, Method method, IntegrationScheduled scheduled) {
        Runnable task = TaskUtils.decorateTaskWithErrorHandler(() ->
                invoke(bean, method), null, false);
        String schedulerName = scheduled.name();
        IntegrationSchedulerRegistry.registerScheduler(schedulerName, task, scheduled);
        IntegrationSchedulerRegistry.scheduleTask(scheduled, tenants);
    }

    @SneakyThrows
    private static void invoke(Object bean, Method method) {
        method.invoke(bean);
    }

    @EventSubscribe(payloadKeyExpression = "*")
    public void onScheduleEvent(ExchangeEvent exchangeEvent) {
        Set<String> entityKeys = new HashSet<>(exchangeEvent.getPayload().keySet());
        List<IntegrationScheduled> integrationSchedules = IntegrationSchedulerRegistry.getIntegrationSchedules();
        if (integrationSchedules != null && !integrationSchedules.isEmpty()) {
            integrationSchedules.forEach(integrationScheduled ->
                    IntegrationSchedulerRegistry.scheduleTask(integrationScheduled, entityKeys));
        }
    }

}
