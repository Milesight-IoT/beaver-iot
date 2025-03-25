package com.milesight.beaveriot.context.mqtt.enums;

import lombok.*;


@Getter
@RequiredArgsConstructor
public enum MqttQos {
    AT_MOST_ONCE(0),
    AT_LEAST_ONCE(1),
    EXACTLY_ONCE(2),
    ;

    private final int value;

    public static MqttQos valueOf(int value) {
        return switch (value) {
            case 0 -> AT_MOST_ONCE;
            case 1 -> AT_LEAST_ONCE;
            case 2 -> EXACTLY_ONCE;
            default -> throw new IllegalArgumentException("invalid QoS: " + value);
        };
    }

    @Override
    public String toString() {
        return name();
    }
}
