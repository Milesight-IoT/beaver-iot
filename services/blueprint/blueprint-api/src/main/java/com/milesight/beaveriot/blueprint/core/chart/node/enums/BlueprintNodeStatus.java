package com.milesight.beaveriot.blueprint.core.chart.node.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BlueprintNodeStatus {
    NOT_READY,
    PENDING,
    FINISHED,
    DELETED,
    ;

    @JsonCreator
    public static BlueprintNodeStatus fromString(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        name = getUpperCaseFirstChar(name);
        return switch (name) {
            case "N" -> NOT_READY;
            case "P" -> PENDING;
            case "F" -> FINISHED;
            case "D" -> DELETED;
            default -> null;
        };
    }

    private static String getUpperCaseFirstChar(String str) {
        return str.toUpperCase().substring(0, 1);
    }

    @JsonValue
    public String toJsonValue() {
        return getUpperCaseFirstChar(name());
    }

    @Override
    public String toString() {
        return name();
    }
}
