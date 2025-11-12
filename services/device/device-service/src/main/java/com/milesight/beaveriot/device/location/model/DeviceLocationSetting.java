package com.milesight.beaveriot.device.location.model;

import com.milesight.beaveriot.base.error.ErrorHolder;
import com.milesight.beaveriot.base.exception.MultipleErrorException;
import com.milesight.beaveriot.base.utils.ValidationUtils;
import com.milesight.beaveriot.context.integration.model.DeviceLocation;
import com.milesight.beaveriot.device.enums.DeviceErrorCode;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * author: Luxb
 * create: 2025/10/23 15:00
 **/
@Data
public class DeviceLocationSetting {
    private String latitude;
    private String longitude;
    private String address;

    public DeviceLocation buildLocation() {
        validate();

        Double latitude = this.latitude == null ? null : Double.parseDouble(this.latitude);
        Double longitude = this.longitude == null ? null : Double.parseDouble(this.longitude);
        return DeviceLocation.of(latitude, longitude, address);
    }

    private void validate() {
        List<ErrorHolder> errors = new ArrayList<>();

        if (this.latitude != null) {
            if (!ValidationUtils.isNumber(this.latitude)) {
                errors.add(ErrorHolder.of(DeviceErrorCode.DEVICE_LOCATION_LATITUDE_TYPE_ERROR));
            }
        }

        if (this.longitude != null) {
            if (!ValidationUtils.isNumber(this.longitude)) {
                errors.add(ErrorHolder.of(DeviceErrorCode.DEVICE_LOCATION_LONGITUDE_TYPE_ERROR));
            }
        }

        if (!errors.isEmpty()) {
            throw MultipleErrorException.with(HttpStatus.BAD_REQUEST.value(), "Validate location error", errors);
        }
    }
}