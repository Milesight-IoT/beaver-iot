package com.milesight.beaveriot.blueprint.library.support;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.milesight.beaveriot.base.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * author: Luxb
 * create: 2025/9/1 13:07
 **/
@Slf4j
public class YamlConverter {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    public static <T> T from(String yamlContent, Class<T> valueType) {
        try {
            if (StringUtils.isEmpty(yamlContent)) {
                return null;
            }

            return MAPPER.readValue(yamlContent, valueType);
        } catch (Exception e) {
            log.error("YamlConverter.from error: {}", e.getMessage(), e);
            return null;
        }
    }
}