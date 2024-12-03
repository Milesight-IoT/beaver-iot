package com.milesight.beaveriot.context.integration.enums;

import com.milesight.beaveriot.context.integration.entity.annotation.Entities;

import java.math.BigDecimal;

/**
 * @author leon
 */
public enum EntityValueType {

    STRING, LONG, DOUBLE, BOOLEAN, BINARY, OBJECT;

    public static EntityValueType of(Class<?> type) {
        if (type == String.class) {
            return STRING;
        } else if (type == Integer.class || type == int.class || type == Long.class || type == long.class) {
            return LONG;
        } else if (type == Float.class || type == float.class || type == Double.class || type == double.class || type == BigDecimal.class) {
            return DOUBLE;
        } else if (type == Boolean.class || type == boolean.class) {
            return BOOLEAN;
        } else if (type == byte[].class || type == Byte[].class) {
            return BINARY;
        } else if (hasEntitiesAnnotation(type)) {
            return OBJECT;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type + ", please use String, Integer, Float, Boolean, byte[] or Object containing @Entities annotation");
        }
    }

    public static boolean hasEntitiesAnnotation(Class<?> clazz) {
        return clazz.isAnnotationPresent(Entities.class);
    }
}
