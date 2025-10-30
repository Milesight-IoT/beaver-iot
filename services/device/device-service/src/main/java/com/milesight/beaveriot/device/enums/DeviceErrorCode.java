package com.milesight.beaveriot.device.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DeviceErrorCode implements ErrorCodeSpec {
    DEVICE_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND.value()),
    DEVICE_GROUP_NAME_EXISTS(HttpStatus.BAD_REQUEST.value()),
    DEVICE_GROUP_NUMBER_LIMITATION(HttpStatus.BAD_REQUEST.value()),
    DEVICE_LIST_SHEET_STRUCTURE_ERROR(HttpStatus.BAD_REQUEST.value()),
    DEVICE_LIST_SHEET_DEVICE_OVER_LIMITATION(HttpStatus.BAD_REQUEST.value()),
    DEVICE_LIST_SHEET_NO_DEVICE(HttpStatus.BAD_REQUEST.value()),
    DEVICE_LIST_SHEET_PARSING_ERROR(HttpStatus.BAD_REQUEST.value()),
    DEVICE_LIST_SHEET_PARSING_VALUE_ERROR(HttpStatus.BAD_REQUEST.value()),
    DEVICE_LOCATION_LONGITUDE_TYPE_ERROR(HttpStatus.BAD_REQUEST.value()),
    DEVICE_LOCATION_LATITUDE_TYPE_ERROR(HttpStatus.BAD_REQUEST.value()),
    ;

    private final int status;
    private final String errorCode;
    private final String errorMessage;
    private final String detailMessage;

    DeviceErrorCode() {
        this.status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        this.errorCode = name().toLowerCase();
        this.errorMessage = null;
        this.detailMessage = null;
    }

    DeviceErrorCode(int status) {
        this.status = status;
        this.errorCode = name().toLowerCase();
        this.errorMessage = null;
        this.detailMessage = null;
    }

    DeviceErrorCode(int status, String errorMessage, String detailMessage) {
        this.status = status;
        this.errorCode = name().toLowerCase();
        this.errorMessage = errorMessage;
        this.detailMessage = detailMessage;
    }
}
