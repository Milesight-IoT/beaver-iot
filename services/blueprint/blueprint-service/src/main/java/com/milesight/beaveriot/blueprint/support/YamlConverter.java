package com.milesight.beaveriot.blueprint.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.milesight.beaveriot.base.utils.StringUtils;

/**
 * author: Luxb
 * create: 2025/9/1 13:07
 **/
public class YamlConverter {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public static <T> T from(String yamlContent, Class<T> valueType) {
        try {
            if (StringUtils.isEmpty(yamlContent)) {
                return null;
            }

            return MAPPER.readValue(yamlContent, valueType);
        } catch (Exception e) {
            return null;
        }
    }
}