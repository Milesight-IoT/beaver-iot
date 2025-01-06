package com.milesight.beaveriot.context.integration.wrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.base.annotations.SFunction;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.constants.ExchangeContextKeys;
import com.milesight.beaveriot.context.integration.entity.annotation.AnnotationEntityCache;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.base.utils.lambada.LambdaMeta;
import com.milesight.beaveriot.base.utils.lambada.LambdaUtils;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.context.util.ExchangeContextHelper;
import com.milesight.beaveriot.eventbus.EventBus;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.eventbus.enums.EventSource;
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

        public void publish(EventSource eventSource) {

            Assert.notNull(exchangePayload, "ExchangePayload is null, please save value first");

            doPublish(exchangePayload, eventSource);
        }

        public void publish() {
            publish(retrieveEventSource());
        }

        private EventSource retrieveEventSource() {
            ExchangeContextHelper.initializeEventSource(exchangePayload);
            return (EventSource) exchangePayload.get(ExchangeContextKeys.EXCHANGE_EVENT_SOURCE);
        }

        public void publish(EventSource eventSource, Consumer<EventResponse> consumer) {

            Assert.notNull(exchangePayload, "ExchangePayload is null, please save value first");

            EventResponse eventResponse = doHandle(exchangePayload, eventSource);

            consumer.accept(eventResponse);
        }

        public void publish(Consumer<EventResponse> consumer) {
            publish(retrieveEventSource(), consumer);
        }

        protected void doPublish(ExchangePayload exchangePayload, EventSource eventSource) {
            SpringContext.getBean(EventBus.class).publish(ExchangeEvent.of(eventSource, exchangePayload));
        }

        protected EventResponse doHandle(ExchangePayload exchangePayload, EventSource eventSource) {
            return SpringContext.getBean(EventBus.class).handle(ExchangeEvent.of(eventSource, exchangePayload));
        }
    }

}
