package com.milesight.beaveriot.device.location.support;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.context.integration.model.DeviceLocation;
import com.milesight.beaveriot.device.location.constants.DeviceLocationConstants;
import com.milesight.beaveriot.device.location.enums.DeviceLocationErrorCode;

import java.text.MessageFormat;

/**
 * author: Luxb
 * create: 2025/10/13 10:49
 **/
public class DeviceLocationSupport {
    public static String getLocationEntityKey(String deviceKey) {
        return MessageFormat.format(DeviceLocationConstants.KEY_FORMAT_DEVICE_LOCATION, deviceKey);
    }

    public static String getLatitudeEntityKey(String deviceKey) {
        return MessageFormat.format(DeviceLocationConstants.KEY_FORMAT_DEVICE_LATITUDE, deviceKey);
    }

    public static String getLongitudeEntityKey(String deviceKey) {
        return MessageFormat.format(DeviceLocationConstants.KEY_FORMAT_DEVICE_LONGITUDE, deviceKey);
    }

    public static String getAddressEntityKey(String deviceKey) {
        return MessageFormat.format(DeviceLocationConstants.KEY_FORMAT_DEVICE_ADDRESS, deviceKey);
    }

    public static void validate(DeviceLocation location) {
        if (location.getLatitude() == null && location.getLongitude() == null && StringUtils.isEmpty(location.getAddress())) {
            return;
        }

        if (!StringUtils.isEmpty(location.getAddress()) && location.getLatitude() == null && location.getLongitude() == null) {
            throw ServiceException.with(DeviceLocationErrorCode.DEVICE_LOCATION_SETTING_ADDRESS_WITHOUT_LATITUDE_AND_LONGITUDE).build();
        }

        if (location.getLatitude() == null || location.getLongitude() == null) {
            throw ServiceException.with(DeviceLocationErrorCode.DEVICE_LOCATION_LATITUDE_AND_LONGITUDE_NOT_BOTH_PROVIDED).build();
        }
    }
}