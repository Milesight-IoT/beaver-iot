package com.milesight.beaveriot.context.integration.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

/**
 * @author leon
 */
public enum EntityType {
    PROPERTY, SERVICE, EVENT;

    @JsonCreator
    public static EntityType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Type value cannot be null");
        }
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid type: " + value));
    }
}