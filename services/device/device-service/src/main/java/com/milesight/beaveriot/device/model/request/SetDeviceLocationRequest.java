package com.milesight.beaveriot.device.model.request;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/10/13 14:00
 **/
@Data
public class SetDeviceLocationRequest {
    private String address;
    private Double longitude;
    private Double latitude;
}
