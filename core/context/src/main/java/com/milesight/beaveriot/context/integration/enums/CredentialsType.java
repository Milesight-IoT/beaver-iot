package com.milesight.beaveriot.context.integration.enums;

import lombok.*;


@Getter
@RequiredArgsConstructor
public enum CredentialsType {
    MQTT,
    SMTP,
    HTTP,
    ;

    @Override
    public String toString() {
        return name();
    }
}
