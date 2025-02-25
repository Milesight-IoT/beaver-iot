package com.milesight.beaveriot.mqtt.broker.bridge.listener;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttMessageEvent {

    String topic;

    byte[] payload;

}
