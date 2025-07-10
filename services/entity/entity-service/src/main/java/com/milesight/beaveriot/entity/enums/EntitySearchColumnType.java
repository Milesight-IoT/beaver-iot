package com.milesight.beaveriot.entity.enums;

import lombok.*;


@Getter
@RequiredArgsConstructor
public enum EntitySearchColumnType {
    TEXT,
    ENUM,
    ;

    @Override
    public String toString() {
        return name();
    }
}
