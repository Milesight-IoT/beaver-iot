package com.milesight.beaveriot.context.mqtt.enums;

import lombok.*;

@Getter
@RequiredArgsConstructor
public enum MqttTopicChannel {
    DEFAULT("beaver-iot"),
    INTERNAL("beaver-iot-internal"),
    ;

    private final String topicPrefix;

    public static MqttTopicChannel getByTopicPrefix(String topicPrefix) {
        for (MqttTopicChannel value : values()) {
            if (value.getTopicPrefix().equals(topicPrefix)) {
                return value;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name();
    }
}
