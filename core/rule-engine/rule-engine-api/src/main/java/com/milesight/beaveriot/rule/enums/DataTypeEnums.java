package com.milesight.beaveriot.rule.enums;

import org.springframework.util.ObjectUtils;

/**
 * @author leon
 */
public enum DataTypeEnums {

    LONG,
    DOUBLE,
    STRING,
    BOOLEAN,
    OTHER;

    public Object validate(String key, Object value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }

        switch (this) {
            case LONG:
                if (!(value instanceof Number)) {
                    throw new IllegalArgumentException("The payload " + key + " value type is invalid, value is " + value);
                }
                if (value instanceof Double) {
                    return Double.valueOf(value.toString()).longValue();
                }
                break;
            case DOUBLE:
                if (!(value instanceof Number)) {
                    throw new IllegalArgumentException("The payload " + key + " value type is invalid, value is " + value);
                }
                if (value instanceof Long) {
                    return Long.valueOf(value.toString()).doubleValue();
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
        return value;
    }
}
