package com.milesight.beaveriot.context.integration.model;

import lombok.Data;

import java.util.Optional;

/**
 * author: Luxb
 * create: 2025/10/13 11:13
 **/
@Data
public class DeviceLocation {
    private Double latitude;
    private Double longitude;
    private String address;

    public static DeviceLocation of(Double latitude, Double longitude, String address) {
        DeviceLocation deviceLocation = new DeviceLocation();
        deviceLocation.setLatitude(latitude);
        deviceLocation.setLongitude(longitude);
        deviceLocation.setAddress(address);
        return deviceLocation;
    }

    public void formatAddress() {
        address = Optional.ofNullable(address)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
    }
}