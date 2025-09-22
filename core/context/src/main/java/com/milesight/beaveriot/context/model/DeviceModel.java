package com.milesight.beaveriot.context.model;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/9/18 11:19
 **/
@Data
public class DeviceModel {
    private String vendorId;
    private String modelId;

    public static DeviceModel of(String vendorId, String modelId) {
        DeviceModel deviceModel = new DeviceModel();
        deviceModel.setVendorId(vendorId);
        deviceModel.setModelId(modelId);
        return deviceModel;
    }
}
