package com.milesight.beaveriot.rule.components.mqtt;

import lombok.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Function;


@Getter
@RequiredArgsConstructor
public enum MqttPayloadEncodingType {
    UTF8("UTF-8", s -> new String(s, StandardCharsets.UTF_8)),
    BASE64("Base64", Base64.getEncoder()::encodeToString),
    ;

    private final String name;

    private final Function<byte[], String> encoder;

    public String encode(byte[] str) {
        return encoder.apply(str);
    }

    public static MqttPayloadEncodingType fromString(String value) {
        if (value != null && !value.isEmpty()) {
            for (MqttPayloadEncodingType encodingType : values()) {
                if (encodingType.name.equalsIgnoreCase(value)) {
                    return encodingType;
                }
            }
        }
        return UTF8;
    }

    @Override
    public String toString() {
        return name();
    }
}
