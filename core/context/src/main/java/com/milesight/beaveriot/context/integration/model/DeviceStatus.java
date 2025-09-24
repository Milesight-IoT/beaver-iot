package com.milesight.beaveriot.context.integration.model;

/**
 * author: Luxb
 * create: 2025/9/24 15:29
 **/
public enum DeviceStatus {
    ONLINE,
    OFFLINE;

    public static DeviceStatus of(String status) {
        for (DeviceStatus value : values()) {
            if (value.name().equalsIgnoreCase(status)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid device status: " + status);
    }
}
