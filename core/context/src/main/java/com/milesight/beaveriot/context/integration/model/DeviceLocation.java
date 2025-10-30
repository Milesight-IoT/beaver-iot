package com.milesight.beaveriot.context.integration.model;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/10/13 11:13
 **/
@Data
public class DeviceLocation {
    private Double longitude;
    private Double latitude;
    private String address;

    public static DeviceLocation of(String address, Double longitude, Double latitude) {
        DeviceLocation deviceLocation = new DeviceLocation();
        deviceLocation.setAddress(address);
        deviceLocation.setLongitude(longitude);
        deviceLocation.setLatitude(latitude);
        return deviceLocation;
    }

    public void setAddress(String address) {
        if (address != null) {
            address = address.trim();
        }
        this.address = address;
    }
}