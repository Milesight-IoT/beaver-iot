package com.milesight.beaveriot.base.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.milesight.beaveriot.base.exception.YamlException;
import org.springframework.util.ObjectUtils;

import java.util.Map;


public class YamlUtils {

    private YamlUtils() {
    }

    private static final ObjectMapper YAML = YAMLMapper.builder()
            .addModule(new JavaTimeModule())
            .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

    static {
        YAML.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    public static ObjectMapper getObjectMapper() {
        return YAML;
    }

    public static String toYAML(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return YAML.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new YamlException(e);
        }
    }

    public static Map<String, Object> toMap(String yaml) {
        return fromYamlWithType(yaml);
    }

    public static <T> T fromYAML(String yaml, Class<T> type) {
        if (ObjectUtils.isEmpty(yaml)) {
            return null;
        }
        try {
            return YAML.readValue(yaml, type);
        } catch (JsonProcessingException e) {
            throw new YamlException(e);
        }
    }

    public static <T> T fromYAML(String yaml, TypeReference<T> type) {
        if (ObjectUtils.isEmpty(yaml)) {
            return null;
        }
        try {
            return YAML.readValue(yaml, type);
        } catch (JsonProcessingException e) {
            throw new YamlException(e);
        }
    }

    public static <T> T fromYamlWithType(String yaml) {
        if (ObjectUtils.isEmpty(yaml)) {
            return null;
        }
        try {
            return YAML.readValue(yaml, new TypeReference<T>() {
            });
        } catch (JsonProcessingException e) {
            throw new YamlException(e);
        }
    }

    public static JsonNode fromYAML(String yaml) {
        if (ObjectUtils.isEmpty(yaml)) {
            return null;
        }
        try {
            return YAML.readTree(yaml);
        } catch (JsonProcessingException e) {
            throw new YamlException(e);
        }
    }

}
