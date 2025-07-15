package com.milesight.beaveriot.context.support;

import com.milesight.beaveriot.base.utils.ValidationUtils;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;

/**
 * author: Luxb
 * create: 2025/7/15 15:03
 **/
public class EntityValidator {
    public static boolean isMatchType(EntityValueType valueType, Object value) {
        if (value == null) {
            return false;
        }
        return switch (valueType) {
            case DOUBLE -> ValidationUtils.isNumber(value.toString());
            case LONG -> ValidationUtils.isInteger(value.toString());
            case BOOLEAN -> value instanceof Boolean;
            case STRING -> value instanceof String;
            case BINARY -> value instanceof byte[];
            default -> true;
        };
    }
}
