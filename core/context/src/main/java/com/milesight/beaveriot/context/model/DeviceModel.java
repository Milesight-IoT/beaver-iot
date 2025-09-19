package com.milesight.beaveriot.context.model;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/9/18 11:19
 **/
@Data
public class DeviceModel {
    private String vendor;
    private String model;

    public static DeviceModel of(String vendor, String model) {
        DeviceModel deviceModel = new DeviceModel();
        deviceModel.setVendor(vendor);
        deviceModel.setModel(model);
        return deviceModel;
    }
}
