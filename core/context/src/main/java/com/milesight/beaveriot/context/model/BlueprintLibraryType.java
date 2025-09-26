package com.milesight.beaveriot.context.model;

public enum BlueprintLibraryType {
    GITHUB,
    GITLAB,
    ZIP;

    public static BlueprintLibraryType of(String type) {
        for (BlueprintLibraryType value : values()) {
            if (value.name().equalsIgnoreCase(type)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid blueprint library address type: " + type);
    }
}