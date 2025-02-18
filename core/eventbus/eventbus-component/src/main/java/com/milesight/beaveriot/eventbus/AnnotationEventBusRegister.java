package com.milesight.beaveriot.eventbus;

import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.eventbus.api.IdentityKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author leon
 */
@Slf4j
public class AnnotationEventBusRegister implements ApplicationContextAware, SmartInitializingSingleton, DisposableBean {
    private ApplicationContext applicationContext;

    private EventBusDispatcher<? extends Event<? extends IdentityKey>> eventBus;

    public AnnotationEventBusRegister(EventBusDispatcher<? extends Event<? extends IdentityKey>> eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (applicationContext == null) {
            return;
        }
        String[] beanDefinitionNames = applicationContext.getBeanNamesForType(Object.class, false, true);

        for (String beanDefinitionName : beanDefinitionNames) {
            Object bean = null;
            Lazy onBean = applicationContext.findAnnotationOnBean(beanDefinitionName, Lazy.class);
            if (onBean!=null){
                log.trace("EventSubscribe annotation scan, skip @Lazy Bean:{}", beanDefinitionName);
                continue;
            }else {
                bean = applicationContext.getBean(beanDefinitionName);
            }

            registerEventSubscribe(bean, beanDefinitionName);
        }
    }

    @Override
    public void destroy() {
        eventBus.shutdown();
    }

    protected void registerEventSubscribe(Object bean, String beanDefinitionName) {
        Map<Method, EventSubscribe> annotatedMethods = null;
        try {
            annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(),
                    (MethodIntrospector.MetadataLookup<EventSubscribe>) method -> AnnotatedElementUtils.findMergedAnnotation(method, EventSubscribe.class));
        } catch (Exception ex) {
            log.error("EventSubscribe method resolve error for bean[" + beanDefinitionName + "].", ex);
        }
        if (annotatedMethods==null || annotatedMethods.isEmpty()) {
            return;
        }

        for (Map.Entry<Method, EventSubscribe> methodEntry : annotatedMethods.entrySet()) {
            Method executeMethod = methodEntry.getKey();
            EventSubscribe eventSubscribe = methodEntry.getValue();
            Class<?> returnType = executeMethod.getReturnType();

            Assert.isTrue(returnType == void.class || returnType == EventResponse.class, "EventSubscribe method return type must be void or EventResponse:" + executeMethod.toGenericString());
            eventBus.registerAnnotationSubscribe(eventSubscribe, bean, executeMethod);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)  {
        this.applicationContext = applicationContext;
    }
}
