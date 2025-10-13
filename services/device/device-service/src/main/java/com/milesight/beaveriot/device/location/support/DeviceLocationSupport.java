package com.milesight.beaveriot.device.location.support;

import com.milesight.beaveriot.device.location.constants.DeviceLocationConstants;

import java.text.MessageFormat;

/**
 * author: Luxb
 * create: 2025/10/13 10:49
 **/
public class DeviceLocationSupport {
    public static String getAddressKey(String deviceKey) {
        return MessageFormat.format(DeviceLocationConstants.KEY_FORMAT_DEVICE_ADDRESS, deviceKey);
    }

    public static String getLongitudeKey(String deviceKey) {
        return MessageFormat.format(DeviceLocationConstants.KEY_FORMAT_DEVICE_LONGITUDE, deviceKey);
    }

    public static String getLatitudeKey(String deviceKey) {
        return MessageFormat.format(DeviceLocationConstants.KEY_FORMAT_DEVICE_LATITUDE, deviceKey);
    }
}