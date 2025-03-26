package com.milesight.beaveriot.context.mqtt.model;

import com.milesight.beaveriot.context.mqtt.enums.MqttTopicChannel;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttMessage {

    private String fullTopicName;

    private String topicSubPath;

    private MqttTopicChannel mqttTopicChannel;

    private String username;

    private String tenantId;

    private List<String> topicFragments;

    private byte[] payload;

}
