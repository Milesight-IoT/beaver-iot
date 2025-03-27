package com.milesight.beaveriot.context.mqtt.model;

import lombok.*;

/**
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttDisconnectEvent {
    private String tenantId;
    private String clientId;
    private String username;
    private Long ts;
}
