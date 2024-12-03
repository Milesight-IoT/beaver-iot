package com.milesight.beaveriot.eventbus.invoke;

import com.milesight.beaveriot.base.exception.ConfigurationException;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.context.integration.proxy.ExchangePayloadProxy;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.IdentityKey;
import lombok.*;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author leon
 */
public class ListenerParameterResolver {

    public ExchangeEvent resolveEvent(@NonNull Class<? extends ExchangePayload> parameterType, Event<? extends ExchangePayload> event, String[] matchMultiKeys) {
        //filter key
        ExchangePayload payload = event.getPayload();
        ExchangePayload newPayload = ExchangePayload.createFrom(payload, List.of(matchMultiKeys));

        newPayload = new ExchangePayloadProxy<>(newPayload, parameterType).proxy();
        return ExchangeEvent.of(event.getEventType(), newPayload);
    }

    public <T extends Event<? extends IdentityKey>> Class<T> resolveActualEventType(Method method) {
        if (method.getParameterTypes().length == 0) {
            throw new ConfigurationException("EventBus method param-number invalid, method:" + method);
        }

        Class<?> clazz = method.getParameterTypes()[0];

        Assert.isTrue(Event.class.isAssignableFrom(clazz), "The EventBus method input parameter must be an implementation of Event, or an implementation of Event containing generic parameters, method:" + method.toGenericString());

        Class<?> eventClass = clazz;
        if (clazz.isInterface()) {
            Class<?> actualTypeArgument = resolveParameterTypes(method);
            if (ExchangePayload.class.isAssignableFrom(actualTypeArgument)) {
                eventClass = ExchangeEvent.class;
            }
        }

        Assert.notNull(eventClass, "The EventBus method input parameter must be an implementation of Event, or an implementation of Event containing generic parameters, method:" + method.toGenericString());

        //noinspection unchecked
        return (Class<T>) eventClass;
    }

    public Class<?> resolveParameterTypes(Method method) {

        if (method.getGenericParameterTypes()[0] instanceof ParameterizedType parameterizedType) {

            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length == 1) {
                Class<?> actualTypeArgument = (Class<?>) actualTypeArguments[0];
                Assert.isTrue(IdentityKey.class.isAssignableFrom(actualTypeArgument), "parameter type must be an implementation of IdentityKey, method:" + method.toGenericString());
                return actualTypeArgument;
            }
        }
        return null;
    }


}
