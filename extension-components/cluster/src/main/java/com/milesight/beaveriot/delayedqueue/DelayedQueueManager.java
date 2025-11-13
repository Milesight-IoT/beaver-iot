package com.milesight.beaveriot.delayedqueue;

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
public class DelayedQueueManager {
    private final DelayedQueueProvider provider;
    private final List<Advisor> advisors;
    private final Map<String, Object> proxyCache = new ConcurrentHashMap<>();

    public DelayedQueueManager(DelayedQueueProvider provider, ApplicationContext applicationContext) {
        this.provider = provider;
        this.advisors = BeanFactoryUtils.beansOfTypeIncludingAncestors(
                        applicationContext, Advisor.class, true, false
                ).values().stream()
                .filter(advisor -> !(advisor instanceof IntroductionAdvisor))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T> DelayedQueue<T> getDelayedQueue(String queueName) {
        return (DelayedQueue<T>) proxyCache.computeIfAbsent(queueName, k -> {
            DelayedQueue<T> rawQueue = provider.create(queueName);
            ProxyFactory factory = new ProxyFactory(rawQueue);
            factory.setInterfaces(DelayedQueue.class);
            factory.addAdvisors(advisors);
            factory.setProxyTargetClass(true);
            return factory.getProxy(getClass().getClassLoader());
        });
    }
}