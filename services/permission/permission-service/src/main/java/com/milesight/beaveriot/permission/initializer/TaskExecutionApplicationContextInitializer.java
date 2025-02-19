package com.milesight.beaveriot.permission.initializer;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.Executor;

/**
 * @author leon
 */
public class TaskExecutionApplicationContextInitializer implements ApplicationContextInitializer {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

        applicationContext.getBeanFactory().addBeanPostProcessor(new SmartInstantiationAwareBeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

                if (bean instanceof Executor executor)  {
                    Executor ttlExecutor = TtlExecutors.getTtlExecutor(executor);
                    return ttlExecutor;
                } else {
                    return bean;
                }
            }
        });
    }
}
