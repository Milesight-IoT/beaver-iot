package com.milesight.beaveriot.context.mqtt;

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

    private List<String> topicFragments;

    private byte[] payload;

}
