package com.milesight.beaveriot.devicetemplate.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

/**
 * author: Luxb
 * create: 2025/6/16 10:00
 **/
public enum ServerErrorCode implements ErrorCodeSpec {
    INTEGRATION_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "integration_not_found", "Integration not found"),
    DEVICE_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "device_template_not_found", "Device template not found"),
    DEVICE_TEMPLATE_EMPTY(HttpStatus.BAD_REQUEST.value(), "device_template_empty", "Device template empty"),
    DEVICE_TEMPLATE_SCHEMA_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "device_template_schema_not_found", "Device template schema not found"),
    DEVICE_TEMPLATE_VALIDATE_ERROR(HttpStatus.BAD_REQUEST.value(), "device_template_validate_error", "Device template validate error"),
    DEVICE_TEMPLATE_DEFINITION_OUTPUT_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "device_template_definition_output_not_found", "Device template definition output not found"),
    DEVICE_ID_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "device_id_not_found", "Key device_id not found"),
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "device_not_found", "Device not found"),
    JSON_VALIDATE_ERROR(HttpStatus.BAD_REQUEST.value(), "json_validate_error", "Json validate error"),
    DEVICE_ENTITY_VALUE_VALIDATE_ERROR(HttpStatus.BAD_REQUEST.value(), "device_entity_value_validate_error", "Device entity value validate error")
    ;

    private final String errorCode;
    private String errorMessage;
    private String detailMessage;
    private int status = HttpStatus.INTERNAL_SERVER_ERROR.value();

    ServerErrorCode(int status, String errorCode, String errorMessage, String detailMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.status = status;
        this.detailMessage = detailMessage;
    }

    ServerErrorCode(int status, String errorCode) {
        this.errorCode = errorCode;
        this.status = status;
    }

    ServerErrorCode(int status, String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.status = status;
    }

    ServerErrorCode(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    ServerErrorCode(String errorCode) {
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

    @Override
    public String getDetailMessage() {
        return detailMessage;
    }

    @Override
    public int getStatus() {
        return status;
    }
}
