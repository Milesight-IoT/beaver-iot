package com.milesight.beaveriot.device.location.constants;

/**
 * author: Luxb
 * create: 2025/10/13 10:45
 **/
public class DeviceLocationConstants {
    public static final String IDENTIFIER_DEVICE_LOCATION = "@location";
    public static final String IDENTIFIER_DEVICE_LATITUDE = "@latitude";
    public static final String IDENTIFIER_DEVICE_LONGITUDE = "@longitude";
    public static final String IDENTIFIER_DEVICE_ADDRESS = "@address";
    public static final String KEY_FORMAT_DEVICE_LOCATION = "{0}." + IDENTIFIER_DEVICE_LOCATION;
    public static final String KEY_FORMAT_DEVICE_LATITUDE = KEY_FORMAT_DEVICE_LOCATION + "." + IDENTIFIER_DEVICE_LATITUDE;
    public static final String KEY_FORMAT_DEVICE_LONGITUDE = KEY_FORMAT_DEVICE_LOCATION + "." + IDENTIFIER_DEVICE_LONGITUDE;
    public static final String KEY_FORMAT_DEVICE_ADDRESS = KEY_FORMAT_DEVICE_LOCATION + "." + IDENTIFIER_DEVICE_ADDRESS;
}