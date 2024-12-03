package com.milesight.beaveriot.eventbus.invoke;


import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.IdentityKey;
import jakarta.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author leon
 */
public class EventSubscribeInvoker<T extends Event<? extends IdentityKey>> implements EventInvoker<T> {
    private final Class<?> parameterType;
    private final Object bean;
    private final Method executeMethod;
    private final ListenerParameterResolver parameterResolver;

    public EventSubscribeInvoker(Object bean, Method executeMethod, @Nullable Class<?> parameterType, ListenerParameterResolver parameterResolver) {
        this.bean = bean;
        this.executeMethod = executeMethod;
        this.parameterType = parameterType;
        this.parameterResolver = parameterResolver;
    }

    @Override
    public Object invoke(T event, String[] matchMultiKeys) throws InvocationTargetException, IllegalAccessException {
        if (parameterType == null
                || !ExchangePayload.class.isAssignableFrom(parameterType)
                || !(event.getPayload() instanceof ExchangePayload)) {
            return executeMethod.invoke(bean, event);
        }

        @SuppressWarnings("unchecked")
        ExchangeEvent resolveEvent = parameterResolver.resolveEvent((Class<? extends ExchangePayload>) parameterType, (Event<? extends ExchangePayload>) event, matchMultiKeys);
        return executeMethod.invoke(bean, resolveEvent);
    }

    @Override
    public String toString() {
        return "EventSubscribeInvoker{" +
                "executeMethod=" + executeMethod.toGenericString() +
                '}';
    }

}
