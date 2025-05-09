package com.milesight.beaveriot.eventbus.interceptor;

import com.milesight.beaveriot.base.utils.TypeUtil;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.EventInterceptor;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.eventbus.api.IdentityKey;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author leon
 */
public class EventInterceptorChain<T extends Event<? extends IdentityKey>> implements InitializingBean {

    private ObjectProvider<EventInterceptor<T>> objectProvider;

    public EventInterceptorChain(ObjectProvider<EventInterceptor<T>> objectProvider) {
        this.objectProvider = objectProvider;
    }

    private Map<Class<?>, List<EventInterceptor<T>>> interceptorMaps = new HashMap<>();

    public boolean preHandle(T event) {
        if (ObjectUtils.isEmpty(interceptorMaps) || !interceptorMaps.containsKey(event.getClass())) {
            return true;
        }

        for (EventInterceptor<T> interceptor : interceptorMaps.get(event.getClass())) {
            if (interceptor.match(event)) {
                boolean isContinue = interceptor.beforeHandle(event);
                if (!isContinue) {
                    return false;
                }
            }
        }
        return true;
    }

    public void afterHandle(T event, EventResponse eventResponse, Exception exception) throws Exception {

        if (ObjectUtils.isEmpty(interceptorMaps) || !interceptorMaps.containsKey(event.getClass())) {
            if (exception != null) {
                throw exception;
            } else {
                return;
            }
        }

        boolean matchInterceptor = false;
        for (int i = interceptorMaps.get(event.getClass()).size() - 1; i >= 0; i--) {
            EventInterceptor<T> interceptor = interceptorMaps.get(event.getClass()).get(i);
            if (interceptor.match(event)) {
                matchInterceptor = true;
                interceptor.afterHandle(event, eventResponse, exception);
            }
        }

        // If any interceptor throws an exception, we need to rethrow it
        if (exception != null && !matchInterceptor) {
            throw exception;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (objectProvider != null) {
            objectProvider.orderedStream().forEach(interceptor -> {
                Type eventType = TypeUtil.getTypeArgument(AopUtils.getTargetClass(interceptor), 0);
                Assert.isTrue(eventType != null , "EventInterceptor must have a class type that is a subclass of Event, interceptor: " + interceptor.getClass().getName());
                List<EventInterceptor<T>> eventInterceptors = interceptorMaps.computeIfAbsent((Class<?>)eventType, k -> new ArrayList<>());
                eventInterceptors.add(interceptor);
            });
        }
    }
}
