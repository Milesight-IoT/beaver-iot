package com.milesight.beaveriot.context.integration.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

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

    public <T> Object convertValue(T value) {
        if (value == null) {
            return null;
        }

        switch (this) {
            case STRING:
                return value instanceof String ? value : JsonUtils.cast(value, String.class);
            case LONG:
                if (value instanceof Long) {
                    return value;
                }

                Double convertedValue = JsonUtils.cast(value, Double.class);
                if (convertedValue == null) {
                    return null;
                }

                return convertedValue.longValue();
            case DOUBLE:
                return value instanceof Double ? value : JsonUtils.cast(value, Double.class);
            case BOOLEAN:
                return value instanceof Boolean ? value : JsonUtils.cast(value, Boolean.class);
            case BINARY:
                if (value instanceof byte[] bytes) {
                    return bytes;
                } else {
                    return Base64.getDecoder().decode(String.valueOf(value));
                }
            case OBJECT:
                return value;
            default:
                throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).build();
        }
    }

    public static ExchangePayload convertValue(ExchangePayload exchangePayload) {
        if (ObjectUtils.isEmpty(exchangePayload)) {
            return exchangePayload;
        }
        Map<String, Entity> exchangeEntities = exchangePayload.getExchangeEntities();
        exchangePayload.forEach((key, value) -> {
            if (exchangeEntities.containsKey(key)) {
                Entity entity = exchangeEntities.get(key);
                exchangePayload.put(key, entity.getValueType().convertValue(value));
            }
        });
        return exchangePayload;
    }

    public static boolean hasEntitiesAnnotation(Class<?> clazz) {
        return clazz.isAnnotationPresent(Entities.class);
    }

    @JsonCreator
    public static EntityValueType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Type value cannot be null");
        }
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid type: " + value));
    }
}
