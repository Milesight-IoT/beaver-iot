package com.milesight.beaveriot.context.integration.model.event;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author leon
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketEvent implements Serializable {

    private String eventType;
    private Object payload;

    public static WebSocketEvent of(String eventType, Object payload) {
        return new WebSocketEvent(eventType, payload);
    }

    public static class EventType {
        private EventType() {
        }

        public static final String EXCHANGE = "Exchange";
    }

}
