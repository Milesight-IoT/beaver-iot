package com.milesight.beaveriot.context.integration.model.event;


import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.IdentityKey;
import org.springframework.util.StringUtils;

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

        public static final String CALL_SERVICE = "CALL_SERVICE";
        public static final String REPORT_EVENT = "REPORT_EVENT";
        public static final String UPDATE_PROPERTY = "UPDATE_PROPERTY";

        public static String of(EntityType entityType, String assignEventType) {
            if (StringUtils.hasText(assignEventType)) {
                return assignEventType;
            }

            switch (entityType) {
                case SERVICE:
                    return CALL_SERVICE;
                case EVENT:
                    return REPORT_EVENT;
                case PROPERTY:
                    return UPDATE_PROPERTY;
                default:
                    throw new IllegalArgumentException("Unsupported entity type: " + entityType);
            }
        }
    }
}
