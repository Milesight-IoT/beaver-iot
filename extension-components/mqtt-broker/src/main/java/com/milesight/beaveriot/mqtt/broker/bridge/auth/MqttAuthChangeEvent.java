package com.milesight.beaveriot.mqtt.broker.bridge.auth;

import lombok.*;

/**
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttAuthChangeEvent<T> {

    private Type type;

    private T data;

    enum Type {
        ADD,
        REMOVE,
        UPDATE,
        ;
    }
}
