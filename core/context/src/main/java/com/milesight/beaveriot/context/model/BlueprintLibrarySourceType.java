package com.milesight.beaveriot.context.model;

/**
 * author: Luxb
 * create: 2025/9/17 16:40
 **/
public enum BlueprintLibrarySourceType {
    DEFAULT,
    CUSTOM,
    UPLOAD;

    public static BlueprintLibrarySourceType of(String type) {
        for (BlueprintLibrarySourceType value : values()) {
            if (value.name().equalsIgnoreCase(type)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid blueprint library source type: " + type);
    }
}
