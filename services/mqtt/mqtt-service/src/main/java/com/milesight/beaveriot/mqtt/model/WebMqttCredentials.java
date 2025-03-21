package com.milesight.beaveriot.mqtt.model;

import lombok.*;

/**
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebMqttCredentials {

    private String clientId;

    private String username;

    private String password;

}
