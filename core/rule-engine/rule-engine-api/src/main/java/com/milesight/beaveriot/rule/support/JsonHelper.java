package com.milesight.beaveriot.rule.support;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.SneakyThrows;
import org.springframework.util.ObjectUtils;

/**
 * @author leon
 */
public class JsonHelper {

    private static final ObjectMapper JSON = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

    private JsonHelper() {
    }

    @SneakyThrows
    public static String toJSON(Object object) {
        return JSON.writeValueAsString(object);
    }

    public static JsonNode fromJSON(String json) {
        if (ObjectUtils.isEmpty(json)) {
            return null;
        }
        try {
            return JSON.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T fromJSON(String json, Class<T> type) {
        if (ObjectUtils.isEmpty(json)) {
            return null;
        }
        try {
            return JSON.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T fromJSON(String json, TypeReference<T> type) {
        if (ObjectUtils.isEmpty(json)) {
            return null;
        }
        try {
            return JSON.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T readValue(String content, JavaType valueType) {
        if (ObjectUtils.isEmpty(content)) {
            return null;
        }
        try {
            return JSON.readValue(content, valueType);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T cast(Object object, TypeReference<T> typeReference) {
        if (object == null) {
            return null;
        }
        return JSON.convertValue(object, typeReference);
    }

    public static <T> T cast(Object object, Class<T> classType) {
        if (object == null) {
            return null;
        }
        return JSON.convertValue(object, classType);
    }

}
