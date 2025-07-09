package com.milesight.beaveriot.entity.enums;

import lombok.*;

/**
 *
 */
@Getter
@RequiredArgsConstructor
public enum EntityTagMappingOperation {
    ADD,
    OVERWRITE,
    REMOVE,
    REPLACE,
    ;

    @Override
    public String toString() {
        return name();
    }
}
