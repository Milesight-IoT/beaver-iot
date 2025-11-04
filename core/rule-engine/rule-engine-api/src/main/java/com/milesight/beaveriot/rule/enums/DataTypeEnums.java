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
                if (value instanceof Number v) {
                    return v.longValue();
                }
                throw new IllegalArgumentException("The payload " + key + " value type is invalid, value is " + value);
            case DOUBLE:
                if (value instanceof Number v) {
                    return v.doubleValue();
                }
                throw new IllegalArgumentException("The payload " + key + " value type is invalid, value is " + value);
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
