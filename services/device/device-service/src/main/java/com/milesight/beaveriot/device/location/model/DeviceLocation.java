package com.milesight.beaveriot.device.location.model;

import lombok.Builder;
import lombok.Data;

/**
 * author: Luxb
 * create: 2025/10/13 11:13
 **/
@Builder
@Data
public class DeviceLocation {
    private String address;
    private Double longitude;
    private Double latitude;

    public static DeviceLocation of(String address, Double longitude, Double latitude) {
        return DeviceLocation.builder()
                .address(address)
                .longitude(longitude)
                .latitude(latitude)
                .build();
    }
}