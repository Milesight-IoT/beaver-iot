package com.milesight.beaveriot.context.mqtt.model;

import lombok.*;
import org.springframework.lang.Nullable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttBrokerInfo {

    @Nullable
    private String host;

    @Nullable
    private Integer mqttPort;

    @Nullable
    private Integer mqttsPort;

    @Nullable
    private String wsPath;

    @Nullable
    private Integer wsPort;

    @Nullable
    private Integer wssPort;

}
