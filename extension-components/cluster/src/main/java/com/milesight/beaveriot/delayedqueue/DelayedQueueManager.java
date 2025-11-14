package com.milesight.beaveriot.delayedqueue;

import com.milesight.beaveriot.context.api.DelayedQueueServiceProvider;
import com.milesight.beaveriot.context.model.delayedqueue.DelayedQueue;
import org.springframework.aop.Advisor;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * author: Luxb
 * create: 2025/11/13 9:32
 **/
@Component
public class DelayedQueueManager implements DelayedQueueServiceProvider {
    private final DelayedQueueFactory factory;
    private final List<Advisor> advisors;
    private final Map<String, Object> proxyCache = new ConcurrentHashMap<>();

    public DelayedQueueManager(DelayedQueueFactory factory, ApplicationContext applicationContext) {
        this.factory = factory;
        this.advisors = BeanFactoryUtils.beansOfTypeIncludingAncestors(
                        applicationContext, Advisor.class, true, false
                ).values().stream()
                .filter(advisor -> !(advisor instanceof IntroductionAdvisor))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> DelayedQueue<T> getDelayedQueue(String queueName) {
        return (DelayedQueue<T>) proxyCache.computeIfAbsent(queueName, k -> {
            DelayedQueue<T> rawQueue = factory.create(queueName);
            ProxyFactory proxyFactory = new ProxyFactory(rawQueue);
            proxyFactory.setInterfaces(DelayedQueue.class);
            proxyFactory.addAdvisors(advisors);
            proxyFactory.setProxyTargetClass(true);
            proxyFactory.setExposeProxy(true);
            return proxyFactory.getProxy(getClass().getClassLoader());
        });
    }
}