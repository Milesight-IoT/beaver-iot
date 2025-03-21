package com.milesight.beaveriot.pubsub;

import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.pubsub.api.annotation.MessageListener;
import com.milesight.beaveriot.pubsub.api.message.PubSubMessage;
import lombok.extern.slf4j.*;
import lombok.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


@Slf4j
@Component
public class MessageRouter implements BeanPostProcessor {

    private static final List<Subscription<PubSubMessage>> SUBSCRIPTIONS = new CopyOnWriteArrayList<>();

    @SuppressWarnings("unchecked")
    protected <T extends PubSubMessage> void subscribe(Class<T> clazz, Consumer<T> listener) {
        log.debug("register message subscription: {}, listener: {}", clazz.getName(), listener);
        SUBSCRIPTIONS.add(new Subscription<>((Class<PubSubMessage>) clazz, (Consumer<PubSubMessage>) listener));
    }

    public void dispatch(PubSubMessage message) {
        if (message == null) {
            log.error("dispatch message failed, message is null");
            return;
        }

        val originalTenantId = TenantContext.tryGetTenantId().orElse(null);
        if (message.getTenantId() != null) {
            TenantContext.setTenantId(message.getTenantId());
        }

        var clazz = message.getClass();
        boolean subscriptionExists = false;
        for (Subscription<PubSubMessage> subscription : SUBSCRIPTIONS) {
            if (subscription.clazz.isAssignableFrom(clazz)) {
                subscriptionExists = true;
                try {
                    subscription.listener.accept(message);
                } catch (Exception e) {
                    log.error("listener invoke error.", e);
                }
            }
        }
        if (!subscriptionExists) {
            log.warn("dispatch message failed, no subscriber found for message: {}", clazz.getName());
        }

        if (originalTenantId != null && !Objects.equals(originalTenantId, message.getTenantId())) {
            TenantContext.setTenantId(originalTenantId);
        }
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Map<Method, MessageListener> annotatedMethods = null;
        try {
            annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(),
                    (MethodIntrospector.MetadataLookup<MessageListener>) method -> AnnotatedElementUtils.findMergedAnnotation(method, MessageListener.class));
        } catch (Exception ex) {
            log.error("SubscribeMessage method resolve error for bean [" + beanName + "].", ex);
        }
        if (annotatedMethods == null || annotatedMethods.isEmpty()) {
            return bean;
        }

        annotatedMethods.forEach((method, anno) -> {
            if (method.getParameterTypes().length != 1) {
                throw new IllegalArgumentException("MessageListener must have only one parameter.");
            }
            Class<?> parameterType = method.getParameterTypes()[0];
            log.debug("register message subscription: {}, method: {}", parameterType.getName(), method);
            //noinspection unchecked
            SUBSCRIPTIONS.add(new Subscription<>((Class<PubSubMessage>) parameterType, message -> {
                try {
                    method.invoke(bean, message);
                } catch (Exception e) {
                    log.error("MessageListener method invoke error.", e);
                }
            }));
        });
        return bean;
    }

    private record Subscription<T extends PubSubMessage>(Class<T> clazz, Consumer<T> listener) {
    }

}
