package com.milesight.beaveriot.device.location.support;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.device.location.constants.DeviceLocationConstants;
import com.milesight.beaveriot.device.location.enums.DeviceLocationErrorCode;
import com.milesight.beaveriot.context.integration.model.DeviceLocation;

import java.text.MessageFormat;

/**
 * author: Luxb
 * create: 2025/10/13 10:49
 **/
public class DeviceLocationSupport {
    public static String getAddressEntityKey(String deviceKey) {
        return MessageFormat.format(DeviceLocationConstants.KEY_FORMAT_DEVICE_ADDRESS, deviceKey);
    }

    public static String getLongitudeEntityKey(String deviceKey) {
        return MessageFormat.format(DeviceLocationConstants.KEY_FORMAT_DEVICE_LONGITUDE, deviceKey);
    }

    public static String getLatitudeEntityKey(String deviceKey) {
        return MessageFormat.format(DeviceLocationConstants.KEY_FORMAT_DEVICE_LATITUDE, deviceKey);
    }

    public static void validate(DeviceLocation location) {
        if (StringUtils.isEmpty(location.getAddress()) && location.getLongitude() == null && location.getLatitude() == null) {
            return;
        }

        if (!StringUtils.isEmpty(location.getAddress()) && location.getLongitude() == null && location.getLatitude() == null) {
            throw ServiceException.with(DeviceLocationErrorCode.DEVICE_LOCATION_SETTING_ADDRESS_WITHOUT_LONGITUDE_AND_LATITUDE).build();
        }

        if (location.getLongitude() == null || location.getLatitude() == null) {
            throw ServiceException.with(DeviceLocationErrorCode.DEVICE_LOCATION_LONGITUDE_AND_LATITUDE_NOT_BOTH_PROVIDED).build();
        }
    }
}