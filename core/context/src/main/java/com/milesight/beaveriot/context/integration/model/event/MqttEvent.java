package com.milesight.beaveriot.context.integration.model.event;


import lombok.*;

import java.io.Serializable;

/**
 * @author leon
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MqttEvent<T> implements Serializable {

    private EventType eventType;

    private T payload;

    public static <T> MqttEvent<T> of(EventType eventType, T payload) {
        return new MqttEvent<>(eventType, payload);
    }

    public enum EventType {

        EXCHANGE,
        ;

        @Override
        public String toString() {
            return name();
        }
    }

}
