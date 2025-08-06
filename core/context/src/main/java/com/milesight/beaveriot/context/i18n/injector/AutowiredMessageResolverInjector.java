package com.milesight.beaveriot.context.i18n.injector;

import com.milesight.beaveriot.context.i18n.support.MessageResolverBeanSupporter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AutowiredMessageResolverInjector implements BeanPostProcessor {
    private final MessageResolverBeanSupporter messageResolverBeanSupporter;

    public AutowiredMessageResolverInjector(MessageResolverBeanSupporter messageResolverBeanSupporter) {
        this.messageResolverBeanSupporter = messageResolverBeanSupporter;
    }

    @Override
    public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        injectMessageResolvers(bean, targetClass);
        return bean;
    }

    private void injectMessageResolvers(Object bean, Class<?> clazz) {
        messageResolverBeanSupporter.injectMessageResolvers(bean, clazz);
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        return bean;
    }
}