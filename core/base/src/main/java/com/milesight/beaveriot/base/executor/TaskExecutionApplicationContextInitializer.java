package com.milesight.beaveriot.base.executor;

import com.alibaba.ttl.spi.TtlEnhanced;
import com.alibaba.ttl.threadpool.TtlExecutors;
import com.alibaba.ttl.threadpool.agent.TtlAgent;
import lombok.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author leon
 */
public class TaskExecutionApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

        applicationContext.getBeanFactory().addBeanPostProcessor(new SmartInstantiationAwareBeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
                if (bean instanceof ThreadPoolTaskExecutor taskExecutor) {
                    return needWrapTaskExecutor(taskExecutor) ? TtlTaskExecutors.getExecutor(taskExecutor) : taskExecutor;
                } else if (bean instanceof ThreadPoolExecutor executor) {
                    return needWrapTaskExecutor(executor) ? TtlExecutors.getTtlExecutor(executor) : executor;
                } else {
                    return bean;
                }
            }

            private boolean needWrapTaskExecutor(Executor taskExecutor) {
                return !(TtlAgent.isTtlAgentLoaded() || null == taskExecutor || taskExecutor instanceof TtlEnhanced);
            }
        });
    }
}
