package com.milesight.beaveriot.base.executor;

import com.alibaba.ttl.threadpool.TtlExecutors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Automatically registers ExecutorService beans extracted from TaskExecutor beans
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ThreadPoolTaskExecutor.class)
@AutoConfigureAfter(TaskExecutorAutoConfiguration.class)
public class TtlExecutorServiceAutoConfiguration implements BeanPostProcessor, ApplicationContextAware {

    private BeanDefinitionRegistry registry;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.registry = (BeanDefinitionRegistry) applicationContext.getAutowireCapableBeanFactory();
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        // Only process TaskExecutorTtlWrapper beans created by TtlTaskExecutors
        if (!(bean instanceof TtlTaskExecutors.TaskExecutorTtlWrapper taskExecutor)) {
            return bean;
        }

        // Get the underlying ThreadPoolTaskExecutor
        ThreadPoolTaskExecutor threadPoolTaskExecutor = taskExecutor.unwrap();

        // Extract ThreadPoolExecutor from ThreadPoolTaskExecutor
        ThreadPoolExecutor executorService = threadPoolTaskExecutor.getThreadPoolExecutor();

        // Apply TTL enhancement
        ExecutorService ttlExecutorService = TtlExecutors.getTtlExecutorService(executorService);

        // Register ExecutorService bean using BeanDefinition to support @Primary
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ExecutorService.class, () -> ttlExecutorService);

        // Check if this is the primary executor
        boolean isPrimary = TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME.equals(beanName);
        builder.setPrimary(isPrimary);

        String executorServiceBeanName = getExecutorServiceBeanName(beanName);
        registry.registerBeanDefinition(executorServiceBeanName, builder.getBeanDefinition());

        log.debug("Registered ExecutorService bean: {} (primary: {}) from TaskExecutor: {}",
                executorServiceBeanName, isPrimary, beanName);

        return bean;
    }

    private String getExecutorServiceBeanName(String taskExecutorBeanName) {
        if (TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME.equals(taskExecutorBeanName)) {
            return "applicationExecutorService";
        }
        return taskExecutorBeanName + "-executor-service";
    }
}
