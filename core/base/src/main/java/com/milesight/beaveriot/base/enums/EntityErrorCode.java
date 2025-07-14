package com.milesight.beaveriot.base.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

import java.text.MessageFormat;

public enum EntityErrorCode implements ErrorCodeSpec {
    ENTITY_VALUE_NONE(HttpStatus.BAD_REQUEST.value(), "entity_value_none",
            "entity {0} value must not be none"),
    ENTITY_VALUE_NOT_MATCH_TYPE(HttpStatus.BAD_REQUEST.value(), "entity_value_not_match_type",
            "entity {0} value required type {1} but provide type {2}"),
    ENTITY_VALUE_LESS_THAN_MIN(HttpStatus.BAD_REQUEST.value(), "entity_value_less_than_min",
            "entity {0} value is less than min value {1}"),
    ENTITY_VALUE_GRATER_THAN_MAX(HttpStatus.BAD_REQUEST.value(), "entity_value_greater_than_max",
            "entity {0} value is greater than max value {1}"),
    ENTITY_VALUE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST.value(), "entity_value_out_of_range",
            "entity {0} value is out of range [{1}, {2}]"),
    ENTITY_VALUE_LENGTH_SHORTER_THAN_MIN_LENGTH(HttpStatus.BAD_REQUEST.value(), "entity_value_length_shorter_than_min_length",
            "entity {0} value length is shorter than min length {1}"),
    ENTITY_VALUE_LENGTH_LONGER_THAN_MAX_LENGTH(HttpStatus.BAD_REQUEST.value(), "entity_value_length_longer_than_max_length",
            "entity {0} value length is longer than max length {1}"),
    ENTITY_VALUE_LENGTH_OUT_OF_LENGTH_RANGE(HttpStatus.BAD_REQUEST.value(), "entity_value_length_out_of_length_range",
            "entity {0} value length is out of length range [{1}, {2}]"),
    ENTITY_VALUE_LENGTH_OUT_OF_LENGTH_ENUM(HttpStatus.BAD_REQUEST.value(), "entity_value_length_out_of_length_enum",
            "entity {0} value length is out of length range enum {1}"),
    ENTITY_VALUE_NOT_MATCH_FORMAT(HttpStatus.BAD_REQUEST.value(), "entity_value_not_match_format",
            "entity {0} value does not match the format {1}"),
    ENTITY_VALUE_OUT_OF_ENUM(HttpStatus.BAD_REQUEST.value(), "entity_value_out_of_enum",
            "entity {0} value is out of enum {1}"),
    ENTITY_VALUE_VALIDATE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "entity_value_validate_error",
            "entity {0} value validate error: {1}");

    private final String errorCode;
    private final String errorMessage;
    private final int status;

    EntityErrorCode(int status, String errorCode, String errorMessage) {
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String formatMessage(Object... args) {
        return MessageFormat.format(errorMessage, args);
    }

    @Override
    public int getStatus() {
        return status;
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
        return null;
    }
}
