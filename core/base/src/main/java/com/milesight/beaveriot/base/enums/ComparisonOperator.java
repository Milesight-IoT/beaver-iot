package com.milesight.beaveriot.base.enums;

import lombok.*;


@Getter
@RequiredArgsConstructor
public enum ComparisonOperator {
    CONTAINS,
    NOT_CONTAINS,
    START_WITH,
    END_WITH,
    ANY_EQUALS,
    IS_EMPTY,
    IS_NOT_EMPTY,
    EQ,
    NE,
    LT,
    LE,
    GT,
    GE,
    ;

    @Override
    public String toString() {
        return name();
    }
}
