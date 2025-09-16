package com.milesight.beaveriot.devicetemplate.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

import java.text.MessageFormat;

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
    DEVICE_ID_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "device_id_not_found", "Device ID key ''{0}'' not found"),
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "device_not_found", "Device not found"),
    DEVICE_BLUEPRINT_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "device_blueprint_not_found", "Device blueprint not found"),
    DEVICE_BLUEPRINT_CREATION_FAILED(HttpStatus.NOT_FOUND.value(), "device_blueprint_creation_failed", "Device blueprint creation failed"),
    DEVICE_DATA_DECODE_FAILED(HttpStatus.NOT_FOUND.value(), "device_data_decode_failed", "Device data decode failed"),
    DEVICE_DATA_ENCODE_FAILED(HttpStatus.NOT_FOUND.value(), "device_data_encode_failed", "Device data encode failed"),
    DEVICE_DATA_UNKNOWN_TYPE(HttpStatus.NOT_FOUND.value(), "device_data_unknown_type", "Device data unknown type"),
    BLUEPRINT_LIBRARY_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "blueprint_library_not_found", "Blueprint library not found"),
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
