package com.milesight.beaveriot.rule.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * @author leon
 */
public class ComponentParameterConverter {

    private ComponentParameterConverter() {
    }

    public static Map<String, Object> convertParameters(JsonNode parameters) {
        if (parameters == null) {
            return Map.of();
        }

        Map<String, Object> dslParameters = Maps.newHashMap();
        if (parameters.isObject()) {
            ObjectNode objectNode = (ObjectNode) parameters;
            objectNode.fields().forEachRemaining(field -> dslParameters.put(field.getKey(), convertValue(field.getValue())));
        }
        return dslParameters;
    }

    private static Object convertValue(JsonNode value) {
        if (value.isInt()) {
            return value.asInt();
        } else if (value.isDouble() || value.isFloat()) {
            return value.asDouble();
        } else if (value.isBoolean()) {
            return value.asBoolean();
        } else {
            return value.toString();
        }
    }

    public static Object getParameterValue(JsonNode parameters, String parameterName) {
        if (parameters == null || !parameters.has(parameterName)) {
            return null;
        }

        return convertValue(parameters.get(parameterName));
    }

}
