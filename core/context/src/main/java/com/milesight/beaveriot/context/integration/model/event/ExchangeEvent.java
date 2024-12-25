package com.milesight.beaveriot.context.integration.model.event;


import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.enums.EventSource;
import com.milesight.beaveriot.eventbus.api.IdentityKey;

import static com.milesight.beaveriot.context.integration.model.event.ExchangeEvent.EventType.TRANSMIT;

/**
 * @author leon
 */
public class ExchangeEvent implements Event<ExchangePayload> {

    private ExchangePayload exchangePayload;
    /**
     * event type, Currently only Transmit
     */
    private String eventType = TRANSMIT;

    private EventSource eventSource;

    public ExchangeEvent() {
    }

    public ExchangeEvent(EventSource eventSource, ExchangePayload exchangePayload) {
        this.eventSource = eventSource;
        this.exchangePayload = exchangePayload;
    }

    @Override
    public String toString() {
        return "ExchangeEvent{" +
                "exchangePayload=" + exchangePayload +
                ", eventType='" + eventType + '\'' +
                '}';
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public void setPayload(IdentityKey payload) {
        this.exchangePayload = (ExchangePayload) payload;
    }

    @Override
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public ExchangePayload getPayload() {
        return exchangePayload;
    }

    @Override
    public EventSource getEventSource() {
        return eventSource;
    }

    @Override
    public void setEventSource(EventSource eventSource) {
        this.eventSource = eventSource;
    }

    public static ExchangeEvent of(EventSource eventSource, ExchangePayload exchangePayload) {
        return new ExchangeEvent(eventSource, exchangePayload);
    }

    public static ExchangeEvent of(ExchangePayload exchangePayload) {
        return new ExchangeEvent(EventSource.INTEGRATION, exchangePayload);
    }

    public static class EventType {
        private EventType() {
        }

        public static final String TRANSMIT = "Transmit";
    }
}
