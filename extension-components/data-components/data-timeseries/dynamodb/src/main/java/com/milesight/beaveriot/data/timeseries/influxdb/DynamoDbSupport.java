package com.milesight.beaveriot.data.timeseries.influxdb;

import com.milesight.beaveriot.base.utils.ValidationUtils;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * author: Luxb
 * create: 2025/11/4 15:22
 **/
public class DynamoDbSupport {
    public static AttributeValue toAttributeValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String vStr) {
            return AttributeValue.builder().s(vStr).build();
        } else if (value instanceof Long || value instanceof Integer) {
            return AttributeValue.builder().n(String.valueOf(value)).build();
        } else if (value instanceof Double || value instanceof Float) {
            return AttributeValue.builder().n(String.valueOf(value)).build();
        } else if (value instanceof Boolean vBoolean) {
            return AttributeValue.builder().bool(vBoolean).build();
        } else {
            throw new IllegalArgumentException("Invalid dynamodb data type: " + value.getClass().getName());
        }
    }

    public static Object toValue(AttributeValue attributeValue) {
        if (attributeValue == null) {
            return null;
        }

        if (attributeValue.s() != null) {
            return attributeValue.s();
        } else if (attributeValue.n() != null) {
            String numStr = attributeValue.n();
            if (ValidationUtils.isInteger(numStr)) {
                return Long.parseLong(numStr);
            } else {
                return Double.parseDouble(numStr);
            }
        } else if (attributeValue.bool() != null) {
            return attributeValue.bool();
        } else {
            throw new IllegalArgumentException("Invalid dynamodb data type: " + attributeValue.type());
        }
    }
}
