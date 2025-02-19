package com.milesight.beaveriot.scheduler;

import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.TimeZone;
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
        String schedulerName = scheduled.scheduler();

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("integration-scheduled-task-"+schedulerName+"-");
        scheduler.initialize();

        String cronEntity = scheduled.cronEntity();
        String fixedDelayEntity = scheduled.fixedDelayEntity();
        String timeUnitEntity = scheduled.timeUnitEntity();

        String cron = scheduled.cron();
        if (!cronEntity.isEmpty()) {
            cron = entityValueServiceProvider.findValueByKey(cronEntity).toString();
        }
        long fixedDelay = scheduled.fixedDelay();
        if (!fixedDelayEntity.isEmpty()) {
            fixedDelay = Long.parseLong(entityValueServiceProvider.findValueByKey(fixedDelayEntity).toString());
        }
        TimeUnit timeUnit = scheduled.timeUnit();
        if (!timeUnitEntity.isEmpty()) {
            timeUnit = TimeUnit.valueOf(entityValueServiceProvider.findValueByKey(timeUnitEntity).toString());
        }

        if (!cron.isEmpty()) {
            //TODO
            String zone = "";
            TimeZone timeZone = zone.isEmpty() ? TimeZone.getDefault() : TimeZone.getTimeZone(zone);
            scheduler.schedule(task, new CronTrigger(cron, timeZone));
        } else if (fixedDelay >= 0) {
            PeriodicTrigger trigger = new PeriodicTrigger(timeUnit.toMillis(fixedDelay));
            scheduler.schedule(task, trigger);
        }
    }

}
