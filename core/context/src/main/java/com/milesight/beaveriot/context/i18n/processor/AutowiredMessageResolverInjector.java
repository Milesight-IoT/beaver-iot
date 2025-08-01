package com.milesight.beaveriot.context.i18n.processor;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.context.i18n.annotation.AutowiredMessageResolver;
import com.milesight.beaveriot.context.i18n.message.MessageResolver;
import com.milesight.beaveriot.context.i18n.message.MessageResolverFactory;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@Slf4j
@Component
public class AutowiredMessageResolverInjector implements BeanPostProcessor {
    private final MessageResolverFactory messageResolverFactory;

    public AutowiredMessageResolverInjector(MessageResolverFactory messageResolverFactory) {
        this.messageResolverFactory = messageResolverFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        injectMessageResolvers(bean, targetClass);
        return bean;
    }

    private void injectMessageResolvers(Object bean, Class<?> clazz) {
        ReflectionUtils.doWithFields(clazz, field -> {
            AutowiredMessageResolver ann = field.getAnnotation(AutowiredMessageResolver.class);
            if (ann != null) {
                try {
                    if (!MessageResolver.class.isAssignableFrom(field.getType())) {
                        throw new IllegalStateException(
                                "Field annotated with @AutowiredMessageResolver must be of type MessageResolver: " + field);
                    }

                    field.setAccessible(true);

                    String module = ann.value();
                    MessageResolver resolver = StringUtils.isEmpty(module)
                            ? messageResolverFactory.getDefault()
                            : messageResolverFactory.getResolver(module);

                    field.set(bean, resolver);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to inject: " + field, e);
                }
            }
        });
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        return bean;
    }
}