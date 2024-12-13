package com.milesight.beaveriot.rule.support;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

    private JsonHelper() {
    }

    @SneakyThrows
    public static String toJSON(Object object) {
        return JSON.writeValueAsString(object);
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
