package com.milesight.beaveriot.device.model.response;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/10/13 14:00
 **/
@Data
public class DeviceLocationResponse {
    private String address;
    private Double longitude;
    private Double latitude;
}
