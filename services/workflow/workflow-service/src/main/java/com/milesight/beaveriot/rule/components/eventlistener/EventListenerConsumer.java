package com.milesight.beaveriot.rule.components.eventlistener;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.context.util.ExchangeContextHelper;
import com.milesight.beaveriot.entity.rule.GenericExchangeValidator;
import com.milesight.beaveriot.eventbus.EventBusDispatcher;
import com.milesight.beaveriot.eventbus.UniqueListenerCacheKey;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.IdentityKey;
import com.milesight.beaveriot.eventbus.invoke.EventInvoker;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;
import org.springframework.util.Assert;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class EventListenerConsumer extends DefaultConsumer {

    private final EventListenerEndpoint endpoint;
    private final GenericExchangeValidator genericExchangeValidator;
    private final EventBusDispatcher eventBus;

    public EventListenerConsumer(EventListenerEndpoint endpoint, Processor processor, GenericExchangeValidator genericExchangeValidator, EventBusDispatcher eventBus) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        this.genericExchangeValidator = genericExchangeValidator;
        this.eventBus = eventBus;
    }

    @Override
    protected void doStart() throws Exception {

        super.doStart();

        Assert.notEmpty(endpoint.getEntities(), "Entities must be set");

        List<String> entities = endpoint.getEntities();

        UniqueListenerCacheKey uniqueListenerCacheKey = generateListenerCacheKey(entities);

        eventBus.registerDynamicSubscribe(ExchangeEvent.class, uniqueListenerCacheKey, new RuleEngineEventInvoker(uniqueListenerCacheKey));
    }

    private UniqueListenerCacheKey generateListenerCacheKey(List<String> entities) {
        String expressions = entities.stream().collect(Collectors.joining(","));
        return new UniqueListenerCacheKey(endpoint.getEventListenerName(), expressions, null);
    }

    @Override
    protected void doStop() throws Exception {
        eventBus.deregisterDynamicSubscribe(ExchangeEvent.class, generateListenerCacheKey(endpoint.getEntities()));
        super.doStop();
    }

    public class RuleEngineEventInvoker implements EventInvoker<Event<? extends IdentityKey>> {

        private final UniqueListenerCacheKey uniqueListenerCacheKey;

        public RuleEngineEventInvoker(UniqueListenerCacheKey uniqueListenerCacheKey) {
            this.uniqueListenerCacheKey = uniqueListenerCacheKey;
        }

        @Override
        public Object invoke(Event<? extends IdentityKey> event, String[] matchMultiKeys) throws InvocationTargetException, IllegalAccessException {
            log.debug("Invoke EventListener ExchangeEvent: {}", event);

            Exchange exchange = endpoint.createExchange();

            exchange.getIn().setHeader(EventListenerConstants.HEADER_EVENTBUS_PAYLOAD_KEY, uniqueListenerCacheKey.getPayloadKey());
            exchange.getIn().setHeader(EventListenerConstants.HEADER_EVENTBUS_TYPE, uniqueListenerCacheKey.getEventTypes());

            ExchangeEvent exchangeEvent = (ExchangeEvent) event;

            ExchangePayload payload = ExchangePayload.createFrom(exchangeEvent.getPayload(), endpoint.getEntities());

            if (endpoint.isVerifyEntitiesValidation()) {
                genericExchangeValidator.matches(payload);
            }
            exchange.getProperties().putAll(ExchangeContextHelper.getTransmitCamelContext(exchangeEvent.getPayload().getContext()));
            exchange.getIn().setBody(payload);
            try {
                getProcessor().process(exchange);
            } catch (Exception e) {
                getExceptionHandler().handleException("Error processing event", exchange, e);
            }
            return exchange.getIn().getBody();
        }

        @Override
        public String toString() {
            return "RuleEngineEventInvoker{" +
                    "uniqueListenerCacheKey=" + uniqueListenerCacheKey.getId() +
                    '}';
        }
    }

}