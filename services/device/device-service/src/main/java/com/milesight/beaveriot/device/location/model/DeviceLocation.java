package com.milesight.beaveriot.device.location.model;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.device.location.enums.DeviceLocationErrorCode;
import lombok.Builder;
import lombok.Data;

/**
 * author: Luxb
 * create: 2025/10/13 11:13
 **/
@Builder
@Data
public class DeviceLocation {
    private String address;
    private Double longitude;
    private Double latitude;

    public static DeviceLocation of(String address, Double longitude, Double latitude) {
        return DeviceLocation.builder()
                .address(address)
                .longitude(longitude)
                .latitude(latitude)
                .build();
    }

    public void validate() {
        if (address != null) {
            address = address.trim();
        }

        if (StringUtils.isEmpty(address) && longitude == null && latitude == null) {
            return;
        }

        if (!StringUtils.isEmpty(address) && longitude == null && latitude == null) {
            throw ServiceException.with(DeviceLocationErrorCode.DEVICE_LOCATION_SETTING_ADDRESS_WITHOUT_LONGITUDE_AND_LATITUDE).build();
        }

        if (longitude == null || latitude == null) {
            throw ServiceException.with(DeviceLocationErrorCode.DEVICE_LOCATION_LONGITUDE_AND_LATITUDE_NOT_BOTH_PROVIDED).build();
        }
    }
}