package com.milesight.beaveriot.device.model.response;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/10/13 14:00
 **/
@Data
public class DeviceLocationResponse {
    private Double latitude;
    private Double longitude;
    private String address;
}
