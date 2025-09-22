package com.milesight.beaveriot.context.support;

import com.milesight.beaveriot.context.model.DeviceModel;

/**
 * author: Luxb
 * create: 2025/9/18 11:18
 **/
public class DeviceModelSupport {
    public static DeviceModel fromModelKey(String modelKey) {
        if (modelKey == null || !modelKey.contains("@")) {
            return null;
        }

        String[] split = modelKey.split("@");
        if (split.length != 2) {
            return null;
        }

        return DeviceModel.of(split[1], split[0]);
    }

    public static String toModelKey(String vendorId, String modelId) {
        return modelId + "@" + vendorId;
    }
}