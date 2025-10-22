package com.milesight.beaveriot.device.location.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

import java.text.MessageFormat;

/**
 * author: Luxb
 * create: 2025/6/16 10:00
 **/
public enum DeviceLocationErrorCode implements ErrorCodeSpec {
    DEVICE_LOCATION_SETTING_ADDRESS_WITHOUT_LONGITUDE_AND_LATITUDE(HttpStatus.BAD_REQUEST.value(),
            "device_location_setting_address_without_longitude_and_latitude",
            "Device location setting must include longitude and latitude when address is provided"),
    DEVICE_LOCATION_LONGITUDE_AND_LATITUDE_NOT_BOTH_PROVIDED(HttpStatus.BAD_REQUEST.value(),
            "device_location_longitude_and_latitude_not_both_provided",
            "Device location setting must provide longitude and latitude both"),
    ;

    private final String errorCode;
    private String errorMessage;
    private String detailMessage;
    private int status = HttpStatus.INTERNAL_SERVER_ERROR.value();

    DeviceLocationErrorCode(int status, String errorCode, String errorMessage, String detailMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.status = status;
        this.detailMessage = detailMessage;
    }

    DeviceLocationErrorCode(int status, String errorCode) {
        this.errorCode = errorCode;
        this.status = status;
    }

    DeviceLocationErrorCode(int status, String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.status = status;
    }

    DeviceLocationErrorCode(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    DeviceLocationErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    public String formatMessage(Object... args) {
        return MessageFormat.format(errorMessage, args);
    }

    @Override
    public String getDetailMessage() {
        return detailMessage;
    }

    @Override
    public int getStatus() {
        return status;
    }
}
