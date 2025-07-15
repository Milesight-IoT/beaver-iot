package com.milesight.beaveriot.base.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

import java.text.MessageFormat;

public enum EntityErrorCode implements ErrorCodeSpec {
    /**
     * Entity related error codes
     */
    ENTITY_KEY_NULL(HttpStatus.BAD_REQUEST.value(), "entity_key_null",
            "entity key must not be null"),
    ENTITY_TYPE_NULL(HttpStatus.BAD_REQUEST.value(), "entity_type_null",
            "entity {0} type must not be null"),
    ENTITY_VALUE_TYPE_NULL(HttpStatus.BAD_REQUEST.value(), "entity_value_type_null",
            "entity {0} value type must not be null"),
    ENTITY_NAME_EMPTY(HttpStatus.BAD_REQUEST.value(), "entity_name_empty",
            "entity {0} name must not be empty"),
    ENTITY_ACCESS_MOD_NULL(HttpStatus.BAD_REQUEST.value(), "entity_access_mod_null",
            "entity {0} access mod must not be null"),
    ENTITY_ATTACH_TARGET_NULL(HttpStatus.BAD_REQUEST.value(), "entity_attach_target_null",
            "entity {0} attach target must not be null"),
    ENTITY_ATTACH_TARGET_ID_NULL(HttpStatus.BAD_REQUEST.value(), "entity_attach_target_id_null",
            "entity {0} attach target id must not be null"),
    ENTITY_ATTRIBUTE_MIN_INVALID(HttpStatus.BAD_REQUEST.value(), "entity_attribute_min_invalid",
            "entity {0} attribute min must be a number"),
    ENTITY_ATTRIBUTE_MAX_INVALID(HttpStatus.BAD_REQUEST.value(), "entity_attribute_max_invalid",
            "entity {0} attribute max must be a number"),
    ENTITY_ATTRIBUTE_MIN_GREATER_THAN_MAX(HttpStatus.BAD_REQUEST.value(), "entity_attribute_min_greater_than_max",
            "entity {0} attribute min {1} is greater than max {2}"),
    ENTITY_ATTRIBUTE_MIN_LENGTH_INVALID(HttpStatus.BAD_REQUEST.value(), "entity_attribute_min_length_invalid",
            "entity {0} attribute min length must be a positive integer"),
    ENTITY_ATTRIBUTE_MAX_LENGTH_INVALID(HttpStatus.BAD_REQUEST.value(), "entity_attribute_max_length_invalid",
            "entity {0} attribute max length must be a positive integer"),
    ENTITY_ATTRIBUTE_MIN_LENGTH_GREATER_THAN_MAX_LENGTH(HttpStatus.BAD_REQUEST.value(), "entity_attribute_min_length_greater_than_max_length",
            "entity {0} attribute min length {1} is greater than max length {2}"),
    ENTITY_ATTRIBUTE_LENGTH_RANGE_INVALID(HttpStatus.BAD_REQUEST.value(), "entity_attribute_length_range_invalid",
            "entity {0} attribute length range is invalid. It must be in the format ''positive integer,positive integer,...'' like ''3,5,7''"),
    ENTITY_ATTRIBUTE_FRACTION_DIGITS_INVALID(HttpStatus.BAD_REQUEST.value(), "entity_attribute_fraction_digits_invalid",
            "entity {0} attribute fraction digits must be a positive integer"),
    ENTITY_ATTRIBUTE_DEFAULT_VALUE_INVALID(HttpStatus.BAD_REQUEST.value(), "entity_attribute_default_value_invalid",
            "entity {0} attribute default value does not match the value type {1}"),
    ENTITY_ATTRIBUTE_OPTIONAL_INVALID(HttpStatus.BAD_REQUEST.value(), "entity_attribute_optional_invalid",
            "entity {0} attribute optional must be a boolean"),
    ENTITY_ATTRIBUTE_ENUM_INVALID(HttpStatus.BAD_REQUEST.value(), "entity_attribute_enum_invalid",
            "entity {0} attribute enum must be a map"),
    ENTITY_VALIDATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "entity_validation_error",
            "entity {0} validation error: {1}"),
    /**
     * Entity value related error codes
     */
    ENTITY_VALUE_NULL(HttpStatus.BAD_REQUEST.value(), "entity_value_null",
            "entity {0} value must not be null"),
    ENTITY_VALUE_NOT_MATCH_TYPE(HttpStatus.BAD_REQUEST.value(), "entity_value_not_match_type",
            "entity {0} value requires type {1} but was provided type {2}"),
    ENTITY_VALUE_LESS_THAN_MIN(HttpStatus.BAD_REQUEST.value(), "entity_value_less_than_min",
            "entity {0} value is less than the minimum allowed value {1}"),
    ENTITY_VALUE_GREATER_THAN_MAX(HttpStatus.BAD_REQUEST.value(), "entity_value_greater_than_max",
            "entity {0} value is greater than the maximum allowed value {1}"),
    ENTITY_VALUE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST.value(), "entity_value_out_of_range",
            "entity {0} value is out of valid range [{1}, {2}]"),
    ENTITY_VALUE_LENGTH_SHORTER_THAN_MIN_LENGTH(HttpStatus.BAD_REQUEST.value(), "entity_value_length_shorter_than_min_length",
            "entity {0} value length is shorter than the minimum allowed length {1}"),
    ENTITY_VALUE_LENGTH_LONGER_THAN_MAX_LENGTH(HttpStatus.BAD_REQUEST.value(), "entity_value_length_longer_than_max_length",
            "entity {0} value length is longer than the maximum allowed length {1}"),
    ENTITY_VALUE_LENGTH_OUT_OF_RANGE(HttpStatus.BAD_REQUEST.value(), "entity_value_length_out_of_range",
            "entity {0} value length is out of valid range [{1}, {2}]"),
    ENTITY_VALUE_LENGTH_INVALID_ENUM(HttpStatus.BAD_REQUEST.value(), "entity_value_length_invalid_enum",
            "entity {0} value length must be one of the allowed lengths {1}"),
    ENTITY_VALUE_NOT_MATCH_FORMAT(HttpStatus.BAD_REQUEST.value(), "entity_value_not_match_format",
            "entity {0} value does not match the required format {1}"),
    ENTITY_VALUE_INVALID_ENUM(HttpStatus.BAD_REQUEST.value(), "entity_value_invalid_enum",
            "entity {0} value is not a valid option in the enum {1}"),
    ENTITY_VALUE_VALIDATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "entity_value_validation_error",
            "entity {0} value validation error: {1}");

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
