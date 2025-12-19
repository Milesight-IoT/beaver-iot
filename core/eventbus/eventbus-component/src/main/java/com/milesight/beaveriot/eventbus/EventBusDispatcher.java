package com.milesight.beaveriot.eventbus;

import com.milesight.beaveriot.base.exception.EventBusExecutionException;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.eventbus.api.IdentityKey;
import com.milesight.beaveriot.eventbus.configuration.ExecutionOptions;
import com.milesight.beaveriot.eventbus.interceptor.EventInterceptorChain;
import com.milesight.beaveriot.eventbus.invoke.EventInvoker;
import com.milesight.beaveriot.eventbus.invoke.EventSubscribeInvoker;
import com.milesight.beaveriot.eventbus.invoke.ListenerParameterResolver;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;


/**
 * @author leon
 */
@Slf4j
public class EventBusDispatcher<T extends Event<? extends IdentityKey>> implements EventBus<T>, ApplicationContextAware {

    private final Map<Class<T>, Map<ListenerCacheKey, List<EventInvoker<T>>>> annotationSubscribeCache = new ConcurrentHashMap<>();
    private final Map<Class<T>, Map<UniqueListenerCacheKey, EventInvoker<T>>> dynamicSubscribeCache = new ConcurrentHashMap<>();
    private ExecutionOptions executionOptions;
    private ListenerParameterResolver parameterResolver;
    private ApplicationContext applicationContext;

    private EventInterceptorChain<T> eventInterceptorChain;

    public EventBusDispatcher(ExecutionOptions executionOptions, ListenerParameterResolver parameterResolver, EventInterceptorChain<T> eventInterceptorChain) {
        this.executionOptions = executionOptions;
        this.parameterResolver = parameterResolver;
        this.eventInterceptorChain = eventInterceptorChain;
    }

    @Override
    public void publish(T event) {
        List<InvocationHolder> invocationHolders = createInvocationHolders(event);
        if (ObjectUtils.isEmpty(invocationHolders)) {
            return;
        }

        Executor executor = getEventBusExecutor();

        log.debug("Ready to publish EventBus events {}, hit Invocation size：{}", event.getEventType(), invocationHolders.size());

        boolean isContinue = eventInterceptorChain.preHandle(event);
        if (!isContinue) {
            log.warn("EventInterceptor preHandle return false, skip publish event: {}", event.getPayloadKey());
            return;
        }

        invocationHolders.forEach(invocationHolder -> executor.execute(() -> {
            try {
                log.debug("Publish EventBus events, match invocation key: {}, invoker: {}", invocationHolder.getMatchMultiKeys(), invocationHolder.getInvoker());
                invocationHolder.getInvoker().invoke(event, invocationHolder.getMatchMultiKeys());
            } catch (Throwable e) {
                log.error("EventSubscribe method invoke error, method: {}", e);
            }
        }));
    }

    private Executor getEventBusExecutor() {
        String eventBusTaskExecutor = executionOptions.getEventBusTaskExecutor();
        if (ObjectUtils.isEmpty(eventBusTaskExecutor) || !applicationContext.containsBean(eventBusTaskExecutor)) {
            throw new EventBusExecutionException("EventBusTaskExecutor not found");
        }
        return (Executor) applicationContext.getBean(eventBusTaskExecutor);
    }

    @SneakyThrows
    @Override
    public EventResponse handle(T event) {

        EventResponse eventResponses = new EventResponse();

        List<Throwable> causes = new ArrayList<>();

        List<InvocationHolder> invocationHolders = createInvocationHolders(event);

        log.debug("Ready to handle EventBus events, hit Invocation size：{}", invocationHolders.size());

        if (!ObjectUtils.isEmpty(invocationHolders)) {
            boolean isContinue = eventInterceptorChain.preHandle(event);
            if (!isContinue) {
                log.warn("EventInterceptor preHandle return false, skip handle event: {}", event.getPayloadKey());
                return eventResponses;
            }
        }

        invocationHolders.forEach(invocationHolder -> {
            try {
                log.debug("Handle EventBus events, match invocation key: {}, invoker: {}", invocationHolder.getMatchMultiKeys(), invocationHolder.getInvoker());
                Object invoke = invocationHolder.getInvoker().invoke(event, invocationHolder.getMatchMultiKeys());
                if (eventResponses != null && invoke instanceof EventResponse eventResponse) {
                    eventResponses.putAll(eventResponse);
                }
            } catch (Throwable e) {
                Throwable throwable = e.getCause() != null ? e.getCause() : e;
                causes.add(throwable);
                log.error("EventSubscribe method invoke error, method: {}", invocationHolder, e);
            }
        });

        if (!CollectionUtils.isEmpty(causes)) {
            EventBusExecutionException exception = new EventBusExecutionException("EventSubscribe method invoke error", causes);
            eventInterceptorChain.afterHandle(event, eventResponses, exception);
        } else {
            eventInterceptorChain.afterHandle(event, eventResponses, null);
        }
        return eventResponses;
    }

    private List<InvocationHolder> createInvocationHolders(T event) {
        Map<ListenerCacheKey, List<EventInvoker<T>>> listenerCacheKeyListMap = annotationSubscribeCache.get(event.getClass());

        List<InvocationHolder> invocationHolders = new ArrayList<>();

        //invoke annotation subscribe
        if (!ObjectUtils.isEmpty(listenerCacheKeyListMap)) {
            List<InvocationHolder> annotationInvocationHolders = createInvocationHoldersFromMultiInvoker(listenerCacheKeyListMap, event);
            invocationHolders.addAll(annotationInvocationHolders);
        }

        //invoke dynamic subscribe
        if (dynamicSubscribeCache.containsKey(event.getClass())) {
            List<InvocationHolder> dynamicInvocationHolders = createInvocationHoldersFromUniqueInvoker(dynamicSubscribeCache.get(event.getClass()), event);
            invocationHolders.addAll(dynamicInvocationHolders);
        }
        return invocationHolders;
    }

    private List<InvocationHolder> createInvocationHoldersFromUniqueInvoker(Map<UniqueListenerCacheKey, EventInvoker<T>> uniqueListenerCacheKeyEventInvokerMap, T event) {
        List<InvocationHolder> invocationHolders = new ArrayList<>();
        for (Map.Entry<UniqueListenerCacheKey, EventInvoker<T>> cacheSubscribeEntry : uniqueListenerCacheKeyEventInvokerMap.entrySet()) {
            String[] matchMultiKeys = filterMatchMultiKeys(event, cacheSubscribeEntry.getKey());
            if (!ObjectUtils.isEmpty(matchMultiKeys)) {
                invocationHolders.add(new InvocationHolder(cacheSubscribeEntry.getValue(), matchMultiKeys, event));
            }
        }
        return invocationHolders;
    }

    private List<InvocationHolder> createInvocationHoldersFromMultiInvoker(Map<ListenerCacheKey, List<EventInvoker<T>>> listenerCacheKeyListMap, T event) {
        List<InvocationHolder> invocationHolders = new ArrayList<>();
        for (Map.Entry<ListenerCacheKey, List<EventInvoker<T>>> listenerCacheKeyListEntry : listenerCacheKeyListMap.entrySet()) {
            String[] matchMultiKeys = filterMatchMultiKeys(event, listenerCacheKeyListEntry.getKey());
            if (!ObjectUtils.isEmpty(matchMultiKeys)) {
                invocationHolders.addAll(listenerCacheKeyListEntry.getValue().stream().map(invoker -> new InvocationHolder(invoker, matchMultiKeys, event)).toList());
            }
        }
        return invocationHolders;
    }

    public void registerDynamicSubscribe(Class<T> eventClass, UniqueListenerCacheKey listenerCacheKey, EventInvoker<T> eventInvoker) {
        dynamicSubscribeCache.computeIfAbsent(eventClass, k -> new ConcurrentHashMap<>()).put(listenerCacheKey, eventInvoker);
    }

    public void deregisterDynamicSubscribe(Class<T> eventClass, UniqueListenerCacheKey listenerCacheKey) {
        if (dynamicSubscribeCache.containsKey(eventClass)) {
            dynamicSubscribeCache.get(eventClass).remove(listenerCacheKey);
        }
    }

    public void registerAnnotationSubscribe(EventSubscribe eventSubscribe, Object bean, Method executeMethod) {

        registerAnnotationSubscribe(eventSubscribe.payloadKeyExpression(), eventSubscribe.eventType(), bean, executeMethod);
    }

    public void registerAnnotationSubscribe(String keyExpression, String[] eventType, Object bean, Method executeMethod) {

        Class<?> parameterTypes = parameterResolver.resolveParameterTypes(executeMethod);

        Class<T> eventClass = parameterResolver.resolveActualEventType(executeMethod);

        ListenerCacheKey listenerCacheKey = new ListenerCacheKey(keyExpression, eventType);

        log.debug("registerAsyncSubscribe: {}, subscriber expression: {}", executeMethod, listenerCacheKey);

        annotationSubscribeCache.computeIfAbsent(eventClass, k -> new ConcurrentHashMap<>());

        annotationSubscribeCache.get(eventClass).computeIfAbsent(listenerCacheKey, k -> new ArrayList<>()).add(new EventSubscribeInvoker<>(bean, executeMethod, parameterTypes, parameterResolver));
    }

    private String[] filterMatchMultiKeys(T event, ListenerCacheKey cacheKey) {
        if (!cacheKey.matchEventType(event.getEventType())) {
            return new String[0];
        }
        return cacheKey.matchMultiKeys(event.getPayloadKey());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Getter
    @AllArgsConstructor
    public class InvocationHolder {

        private EventInvoker<T> invoker;
        private String[] matchMultiKeys;
        private Event<? extends IdentityKey> event;
    }
}
