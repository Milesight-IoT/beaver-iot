package com.milesight.beaveriot.mqtt.broker.bridge.listener.event;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttMessageEvent {

    String topic;

    byte[] payload;

}
