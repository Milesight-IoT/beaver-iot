package com.milesight.beaveriot.context.integration.wrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.base.annotations.SFunction;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.base.utils.lambada.LambdaMeta;
import com.milesight.beaveriot.base.utils.lambada.LambdaUtils;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.entity.annotation.AnnotationEntityCache;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.context.util.ExchangeContextHelper;
import com.milesight.beaveriot.eventbus.EventBus;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author leon
 */
public abstract class AbstractWrapper {

    protected <S> String parserEntityKey(SFunction<S, ?> keyFun) {

        LambdaMeta extract = LambdaUtils.extract(keyFun);

        Optional<Method> methodOptional = ReflectionUtils.getMethod(extract.getInstantiatedClass(), extract.getImplMethodName());

        Assert.isTrue(!methodOptional.isEmpty(), "Method not found : " + extract.getImplMethodName());

        return AnnotationEntityCache.INSTANCE.getEntityKeyByMethod(methodOptional.get());
    }

    protected void doSaveValue(ExchangePayload exchangePayload, long timestamp) {
        SpringContext.getBean(EntityValueServiceProvider.class).saveValues(exchangePayload, timestamp);
    }

    protected <T> Optional<T> findValueByKey(String key, Class<T> clazz) {
        JsonNode jsonNodeValue = SpringContext.getBean(EntityValueServiceProvider.class).findValueByKey(key);
        if (jsonNodeValue == null) {
            return Optional.empty();
        }
        return Optional.of(JsonUtils.cast(jsonNodeValue, clazz));
    }

    protected Map<String, JsonNode> findValuesByKeys(List<String> keys) {
        return SpringContext.getBean(EntityValueServiceProvider.class).findValuesByKeys(keys);
    }

    public class ExchangeEventPublisher {

        private ExchangePayload exchangePayload;

        public ExchangeEventPublisher(ExchangePayload exchangePayload) {
            this.exchangePayload = exchangePayload;
        }

        public void publish(String eventType) {

            Assert.notNull(exchangePayload, "ExchangePayload is null, please save value first");

            doPublish(exchangePayload, eventType);
        }

        public void publish() {
            publish("");
        }

        public void publish(String eventType, Consumer<EventResponse> consumer) {

            Assert.notNull(exchangePayload, "ExchangePayload is null, please save value first");

            EventResponse eventResponse = doHandle(exchangePayload, eventType);

            consumer.accept(eventResponse);
        }

        public void publish(Consumer<EventResponse> consumer) {
            publish("", consumer);
        }

        protected void doPublish(ExchangePayload exchangePayload, String eventType) {
            EventBus eventBus = SpringContext.getBean(EventBus.class);
            Map<EntityType, ExchangePayload> splitExchangePayloads = exchangePayload.splitExchangePayloads();
            splitExchangePayloads.forEach((entityType, payload) -> {
                String obtainEventType = ExchangeEvent.EventType.of(entityType, eventType);
                ExchangeContextHelper.initializeEventSource(payload);
                ExchangeContextHelper.initializeEventType(payload, obtainEventType);
                eventBus.publish(ExchangeEvent.of(obtainEventType, payload));
            });

        }

        protected EventResponse doHandle(ExchangePayload exchangePayload, String eventType) {
            EventBus eventBus = SpringContext.getBean(EventBus.class);
            Map<EntityType, ExchangePayload> splitExchangePayloads = exchangePayload.splitExchangePayloads();
            EventResponse eventResponse = EventResponse.empty();
            splitExchangePayloads.forEach((entityType, payload) -> {
                String obtainEventType = ExchangeEvent.EventType.of(entityType, eventType);
                ExchangeContextHelper.initializeEventSource(payload);
                ExchangeContextHelper.initializeEventType(payload, obtainEventType);
                EventResponse response = eventBus.handle(ExchangeEvent.of(obtainEventType, payload));
                if (response != null) {
                    eventResponse.putAll(response);
                }
            });
            return eventResponse;
        }
    }

}
