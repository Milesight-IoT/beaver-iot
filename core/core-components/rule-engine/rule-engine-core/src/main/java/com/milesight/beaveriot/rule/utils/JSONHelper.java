package com.milesight.beaveriot.rule.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.SneakyThrows;
import org.springframework.util.ObjectUtils;

/**
 * @author leon
 */
public class JSONHelper {

    private static final ObjectMapper JSON = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

    private JSONHelper() {
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

}
