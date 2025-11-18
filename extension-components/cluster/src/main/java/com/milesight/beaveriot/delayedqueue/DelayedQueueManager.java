package com.milesight.beaveriot.delayedqueue;

import com.milesight.beaveriot.context.api.DelayedQueueServiceProvider;
import com.milesight.beaveriot.context.model.delayedqueue.DelayedQueue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * author: Luxb
 * create: 2025/11/13 9:32
 **/
@Component
public class DelayedQueueManager implements DelayedQueueServiceProvider {
    private final DelayedQueueFactory factory;
    private final ConfigurableApplicationContext applicationContext;

    public DelayedQueueManager(DelayedQueueFactory factory, ApplicationContext applicationContext) {
        this.factory = factory;
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> DelayedQueue<T> getDelayedQueue(String queueName) {
        String beanName = getBeanName(queueName);

        if (!applicationContext.containsBean(beanName)) {
            DelayedQueue<T> delayedQueue = factory.create(queueName);
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition((Class<DelayedQueue<T>>) delayedQueue.getClass(), () -> delayedQueue);
            beanFactory.registerBeanDefinition(beanName, builder.getBeanDefinition());
        }
        return applicationContext.getBean(beanName, DelayedQueue.class);
    }

    private String getBeanName(String queueName) {
        return Constants.DELAYED_QUEUE_BEAN_NAME_PREFIX + queueName;
    }

    private static class Constants {
        public static final String DELAYED_QUEUE_BEAN_NAME_PREFIX = "delayed-queue:";
    }
}