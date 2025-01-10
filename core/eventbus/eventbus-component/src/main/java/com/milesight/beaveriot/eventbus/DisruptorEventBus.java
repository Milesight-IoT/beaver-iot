package com.milesight.beaveriot.eventbus;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.milesight.beaveriot.base.exception.EventBusExecutionException;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.eventbus.api.IdentityKey;
import com.milesight.beaveriot.eventbus.configuration.DisruptorOptions;
import com.milesight.beaveriot.eventbus.invoke.EventInvoker;
import com.milesight.beaveriot.eventbus.invoke.EventSubscribeInvoker;
import com.milesight.beaveriot.eventbus.invoke.ListenerParameterResolver;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * @author leon
 */
@Slf4j
public class DisruptorEventBus<T extends Event<? extends IdentityKey>> implements EventBus<T>, ApplicationContextAware {

    private final Map<Class<T>, Map<ListenerCacheKey, List<EventInvoker<T>>>> annotationSubscribeCache = new ConcurrentHashMap<>();
    private final Map<Class<T>, Map<UniqueListenerCacheKey, EventInvoker<T>>> dynamicSubscribeCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Disruptor<Event<?>>> disruptorCache = new ConcurrentHashMap<>();
    private DisruptorOptions disruptorOptions;
    private ListenerParameterResolver parameterResolver;
    private ApplicationContext applicationContext;

    public DisruptorEventBus(DisruptorOptions disruptorOptions, ListenerParameterResolver parameterResolver) {
        this.disruptorOptions = disruptorOptions;
        this.parameterResolver = parameterResolver;
    }

    @Override
    public void publish(T message) {

        Disruptor<Event<?>> disruptor = disruptorCache.get(message.getClass());

        if (disruptor == null) {
            log.debug("disruptor is null, please subscribe first, event: {}", message.getClass());
            return;
        }

        disruptor.getRingBuffer().publishEvent((event, sequence) -> {
            if (message instanceof Copyable copyable) {
                copyable.copy(message, event);
            } else {
                IdentityKey payload = message.getPayload();
                event.setPayload(payload);
                event.setEventType(message.getEventType());
            }
        });
    }

    @Override
    public void subscribe(Class<T> target, Executor executor, Consumer<T>... listener) {

        disruptorCache.computeIfAbsent(target, k -> {

            Disruptor<Event<?>> disruptor = new Disruptor<>(() -> loadClass(target), disruptorOptions.getRingBufferSize(), executor);

            EventHandler[] eventHandlers = Arrays.stream(listener).map(ls -> (EventHandler<T>) (o, l, b) -> ls.accept(o)).toArray(EventHandler[]::new);

            disruptor.handleEventsWith(eventHandlers);

            disruptor.start();

            return disruptor;
        });
    }

    @Override
    public EventResponse handle(T event) {

        EventResponse eventResponse = new EventResponse();

        Map<ListenerCacheKey, List<EventInvoker<T>>> listenerCacheKeyListMap = annotationSubscribeCache.get(event.getClass());

        if (listenerCacheKeyListMap == null) {
            log.warn("no subscribe handler for event: {}", event.getEventType());
            return null;
        }

        List<Throwable> causes = new ArrayList<>();

        //invoke annotation subscribe
        for (Map.Entry<ListenerCacheKey, List<EventInvoker<T>>> listenerCacheKeyListEntry : listenerCacheKeyListMap.entrySet()) {
            createSyncConsumer(listenerCacheKeyListEntry.getKey(), listenerCacheKeyListEntry.getValue(), eventResponse, causes).accept(event);
        }
        //invoke dynamic subscribe
        if (dynamicSubscribeCache.containsKey(event.getClass())) {
            createDynamicAsyncConsumer().accept(event);
        }

        if (!CollectionUtils.isEmpty(causes)) {
            throw new EventBusExecutionException("EventSubscribe method invoke error", causes);
        }

        return eventResponse;
    }

    @Override
    public void shutdown() {
        disruptorCache.values().forEach(Disruptor::shutdown);
    }

    @Override
    public void subscribe(Class<T> target, Consumer<T>... listener) {

        Executor executor = (Executor) applicationContext.getBean(disruptorOptions.getEventBusTaskExecutor());

        subscribe(target, executor, listener);
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

    public void fireAsyncSubscribe() {
        annotationSubscribeCache.forEach((k, v) -> {
            List<Consumer<T>> allConsumers = v.entrySet().stream().map(entry -> createAnnotationAsyncConsumer(entry.getKey(), entry.getValue())).collect(Collectors.toList());
            allConsumers.add(createDynamicAsyncConsumer());
            subscribe(k, allConsumers.toArray(new Consumer[0]));
        });
    }

    private Consumer<T> createSyncConsumer(ListenerCacheKey cacheKey, List<EventInvoker<T>> invokers, @Nullable EventResponse eventResponses, List<Throwable> causes) {
        return event -> {
            String[] matchMultiKeys = filterMatchMultiKeys(event, cacheKey);
            if (ObjectUtils.isEmpty(matchMultiKeys)) {
                return;
            }

            invokers.forEach(invoker -> {
                try {
                    Object invoke = invoker.invoke(event, matchMultiKeys);
                    if (eventResponses != null && invoke instanceof EventResponse eventResponse) {
                        eventResponses.putAll(eventResponse);
                    }
                } catch (Throwable e) {
                    Throwable throwable = e.getCause() != null ? e.getCause() : e;
                    causes.add(throwable);
                    log.error("EventSubscribe method invoke error, method: {}", invoker, e);
                }
            });
        };
    }

    private Consumer<T> createDynamicAsyncConsumer() {
        return event -> {
            Map<UniqueListenerCacheKey, EventInvoker<T>> subscribeCache = dynamicSubscribeCache.get(event.getClass());
            if (ObjectUtils.isEmpty(subscribeCache)) {
                return;
            }

            for (Map.Entry<UniqueListenerCacheKey, EventInvoker<T>> cacheSubscribeEntry : subscribeCache.entrySet()) {
                String[] matchMultiKeys = filterMatchMultiKeys(event, cacheSubscribeEntry.getKey());
                if (ObjectUtils.isEmpty(matchMultiKeys)) {
                    continue;
                }
                try {
                    cacheSubscribeEntry.getValue().invoke(event, matchMultiKeys);
                } catch (Exception e) {
                    log.error("Dynamic EventSubscribe invoke error, method: {}", e);
                }
            }
        };
    }

    private Consumer<T> createAnnotationAsyncConsumer(ListenerCacheKey cacheKey, List<EventInvoker<T>> invokers) {
        return event -> {
            String[] matchMultiKeys = filterMatchMultiKeys(event, cacheKey);
            if (ObjectUtils.isEmpty(matchMultiKeys)) {
                return;
            }
            invokers.forEach(invoker -> {
                try {
                    invoker.invoke(event, matchMultiKeys);
                } catch (Exception e) {
                    log.error("EventSubscribe method invoke error, method: {}", invoker, e);
                }
            });
        };
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

    private <E> E loadClass(Class<E> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new EventBusExecutionException("Class load exception", e);
        }
    }
}
