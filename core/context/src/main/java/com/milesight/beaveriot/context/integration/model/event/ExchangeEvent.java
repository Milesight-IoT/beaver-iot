package com.milesight.beaveriot.context.integration.model.event;


import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.IdentityKey;

/**
 * @author leon
 */
public class ExchangeEvent implements Event<ExchangePayload> {

    private ExchangePayload exchangePayload;
    private String eventType;

    public ExchangeEvent() {
    }

    public ExchangeEvent(String eventType, ExchangePayload exchangePayload) {
        this.eventType = eventType;
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

    public static ExchangeEvent of(String eventType, ExchangePayload exchangePayload) {
        return new ExchangeEvent(eventType, exchangePayload);
    }

    public static class EventType {
        private EventType() {
        }

        public static final String DOWN = "Down";
        public static final String UP = "Up";
    }
}
