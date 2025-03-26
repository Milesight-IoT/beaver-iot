package com.milesight.beaveriot.mqtt.broker.bridge.listener.event;

import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttClientDisconnectEvent {
    private String clientId;
    private String username;
    private Long ts;
}
