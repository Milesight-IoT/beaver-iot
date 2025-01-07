package com.milesight.beaveriot.rule.enums;

import org.springframework.util.ObjectUtils;

/**
 * @author leon
 */
public enum DataTypeEnums {

    INT,
    FLOAT,
    STRING,
    BOOLEAN,
    OTHER;

    public void validate(String key, Object value) {
        if (ObjectUtils.isEmpty(value)) {
            return;
        }

        switch (this) {
            case INT:
            case FLOAT:
                if (!(value instanceof Number)) {
                    throw new IllegalArgumentException("The payload " + key + " value type is invalid, value is " + value);
                }
                break;
            case STRING:
                if (!(value instanceof String)) {
                    throw new IllegalArgumentException("The payload " + key + " value type is invalid, value is " + value);
                }
                break;
            case BOOLEAN:
                if (!(value instanceof Boolean)) {
                    throw new IllegalArgumentException("The payload " + key + " value type is invalid, value is " + value);
                }
                break;
            default:
                break;
        }
    }
}
