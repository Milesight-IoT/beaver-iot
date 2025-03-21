package com.milesight.beaveriot.context.integration.enums;

import lombok.*;


@Getter
@RequiredArgsConstructor
public enum CredentialsType {
    MQTT,
    ;

    @Override
    public String toString() {
        return name();
    }
}
