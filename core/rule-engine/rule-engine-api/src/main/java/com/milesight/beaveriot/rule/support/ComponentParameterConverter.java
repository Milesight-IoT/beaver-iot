package com.milesight.beaveriot.rule.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.milesight.beaveriot.rule.model.definition.ComponentDefinition;
import com.milesight.beaveriot.rule.model.definition.ComponentOptionDefinition;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author leon
 */
@Slf4j
public class ComponentParameterConverter {

    private ComponentParameterConverter() {
    }

    public static Map<String, Object> convertParameters(JsonNode parameters, ComponentDefinition componentDefinition) {
        if (parameters == null) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> routeParameters = Maps.newHashMap();
        Map<String, ComponentOptionDefinition> properties = componentDefinition.getProperties();
        if (parameters.isObject()) {
            ObjectNode objectNode = (ObjectNode) parameters;
            objectNode.fields().forEachRemaining(field -> {
                if (properties.containsKey(field.getKey())) {
                    routeParameters.put(properties.get(field.getKey()).generateFullName(), convertValue(field.getValue()));
                } else {
                    log.warn("Component {} does not have property {}", componentDefinition.getComponent().getName(), field.getKey());
                }
            });
        }
        return routeParameters;
    }

    private static Object convertValue(JsonNode value) {
        if (value.isInt()) {
            return value.asInt();
        } else if (value.isDouble() || value.isFloat()) {
            return value.asDouble();
        } else if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.isTextual()) {
            return value.textValue();
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
