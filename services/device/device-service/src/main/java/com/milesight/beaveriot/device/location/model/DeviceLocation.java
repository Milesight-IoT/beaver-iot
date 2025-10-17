package com.milesight.beaveriot.device.location.model;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.device.constants.DeviceDataFieldConstants;
import com.milesight.beaveriot.device.location.enums.DeviceLocationErrorCode;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

/**
 * author: Luxb
 * create: 2025/10/13 11:13
 **/
@Data
public class DeviceLocation {
    @Range(min = -180, max = 180)
    private Double longitude;

    @Range(min = -90, max = 90)
    private Double latitude;

    @Size(max = DeviceDataFieldConstants.DEVICE_ADDRESS_MAX_LENGTH)
    private String address;

    public static DeviceLocation of(String address, Double longitude, Double latitude) {
        DeviceLocation deviceLocation = new DeviceLocation();
        deviceLocation.setAddress(address);
        deviceLocation.setLongitude(longitude);
        deviceLocation.setLatitude(latitude);
        return deviceLocation;
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