
package com.milesight.beaveriot.base.executor;

import com.milesight.beaveriot.base.utils.StringUtils;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.aop.interceptor.AsyncExecutionAspectSupport;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leon
 */
@ConditionalOnClass(ThreadPoolTaskExecutor.class)
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TaskExecutorProperties.class)
@AutoConfigureBefore(TaskExecutionAutoConfiguration.class)
@Import(TaskExecutorAutoConfiguration.TaskExecutorRegistrar.class)
public class TaskExecutorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TaskExecutorShutdown threadPoolExecutorShutdown() {
        return new TaskExecutorShutdown();
    }

    @Slf4j
    public static class TaskExecutorRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

        private Environment environment;

        @Override
        public void registerBeanDefinitions(@NonNull AnnotationMetadata metadata,
                                            @NonNull BeanDefinitionRegistry registry) {
            log.debug("registering task executors...");

            List<String> registerTaskExecutors = new ArrayList<>();
            var properties = Binder.get(environment).bind(TaskExecutorProperties.TASK_EXECUTOR_PROPERTY_PREFIX, TaskExecutorProperties.class);
            if (properties.isBound()) {
                var taskExecutionProperties = properties.get();
                taskExecutionProperties.getPool().forEach((key, value) -> {
                    boolean isPrimary = TaskExecutorProperties.PRIMARY_EXECUTOR_ID.equals(key);
                    if (isPrimary) {
                        key = TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME;
                    }
                    registerDefaultTaskExecutor(registry, taskExecutionProperties, value, key, null, isPrimary);
                    registerTaskExecutors.add(key);
                });
            }

            log.debug("registered task executors: {} ", String.join(",", registerTaskExecutors));
        }

        private void registerDefaultTaskExecutor(BeanDefinitionRegistry registry, TaskExecutorProperties taskExecutionProperties, TaskExecutorProperties.ExecutorPoolProperties executorConfig, String beanName, String[] alias, boolean isPrimary) {
            BeanDefinitionBuilder definitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(TaskExecutorFactoryBean.class);

            int maxQueueCapacity = obtainMaxQueueCapacity(taskExecutionProperties, executorConfig);
            definitionBuilder.addPropertyValue("queueCapacity", maxQueueCapacity);
            definitionBuilder.addPropertyValue("corePoolSize", executorConfig.getCoreSize());
            definitionBuilder.addPropertyValue("maxPoolSize", Math.max(executorConfig.getMaxSize(), executorConfig.getCoreSize()));
            definitionBuilder.addPropertyValue("allowCoreThreadTimeOut", executorConfig.isAllowCoreThreadTimeout());
            definitionBuilder.addPropertyValue("keepAlive", executorConfig.getKeepAlive());
            definitionBuilder.addPropertyValue("rejectedPolicy", executorConfig.getRejectedPolicy());
            definitionBuilder.addPropertyValue("threadNamePrefix", executorConfig.getThreadNamePrefix());
            definitionBuilder.addPropertyValue("transmittable", executorConfig.isTransmittable());
            if (executorConfig.isAwaitTermination() && executorConfig.getAwaitTerminationPeriod() != null) {
                definitionBuilder.addPropertyValue("awaitTermination", executorConfig.isAwaitTermination());
                definitionBuilder.addPropertyValue("awaitTerminationPeriod", executorConfig.getAwaitTerminationPeriod());
            }

            String defaultPrefix;
            if (isPrimary) {
                definitionBuilder.setPrimary(true);
                defaultPrefix = TaskExecutorProperties.DEFAULT_TASK_PREFIX;
            } else {
                defaultPrefix = beanName + "-";
            }

            var prefix = StringUtils.isEmpty(executorConfig.getThreadNamePrefix()) ? defaultPrefix : executorConfig.getThreadNamePrefix();
            definitionBuilder.addPropertyValue("threadNamePrefix", prefix);

            var definition = definitionBuilder.getBeanDefinition();
            definition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, TaskExecutor.class);

            var holder = alias != null && alias.length != 0
                    ? new BeanDefinitionHolder(definition, beanName, alias)
                    : new BeanDefinitionHolder(definition, beanName, new String[]{AsyncExecutionAspectSupport.DEFAULT_TASK_EXECUTOR_BEAN_NAME});
            BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
        }

        private int obtainMaxQueueCapacity(TaskExecutorProperties taskExecutionProperties, TaskExecutorProperties.ExecutorPoolProperties executionConfig) {
            return Math.min(executionConfig.getQueueCapacity(), taskExecutionProperties.getMaxQueueCapacity());
        }

        @Override
        public void setEnvironment(@NonNull Environment environment) {
            this.environment = environment;
        }
    }


}
