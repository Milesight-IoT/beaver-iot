package com.milesight.beaveriot.base.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.milesight.beaveriot.base.exception.JSONException;
import org.springframework.util.ObjectUtils;

import java.util.Map;
import java.util.Objects;

/**
 * json util
 *
 * @author leon
 */
public class JsonUtils {

    private static final Instance WITH_SNAKE_CASE_STRATEGY = new Instance(jsonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build());

    private static final Instance WITH_CAMEL_CASE_STRATEGY = new Instance(jsonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            .build());

    private static final Instance WITH_DEFAULT_STRATEGY = new Instance(jsonMapperBuilder()
            .build());

    private JsonUtils() {

    }

    private static JsonMapper.Builder jsonMapperBuilder() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true)
                .serializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static Instance withCamelCaseStrategy() {
        return WITH_CAMEL_CASE_STRATEGY;
    }

    public static Instance withSnakeCaseStrategy() {
        return WITH_SNAKE_CASE_STRATEGY;
    }

    public static Instance withDefaultStrategy() {
        return WITH_DEFAULT_STRATEGY;
    }

    public static ObjectMapper getObjectMapper() {
        return withSnakeCaseStrategy().objectMapper;
    }

    public static <T> T copy(T object) {
        return cast(object, getClass(object));
    }

    public static <T> T copy(T object, TypeReference<T> typeReference) {
        return cast(object, typeReference);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getClass(T object) {
        return (Class<T>) object.getClass();
    }

    public static <T> T cast(Object object, TypeReference<T> typeReference) {
        if (object == null) {
            return null;
        }
        return getObjectMapper().convertValue(object, typeReference);
    }

    public static <T> T cast(Object object, Class<T> classType) {
        if (object == null) {
            return null;
        }
        return getObjectMapper().convertValue(object, classType);
    }

    public static String toPrettyJSON(String json) {
        return toPrettyJSON(fromJSON(json));
    }

    public static String toJSON(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return getObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JSONException(e);
        }
    }

    public static String toPrettyJSON(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JSONException(e);
        }
    }

    public static Map<String, Object> toMap(String json) {
        return fromJsonWithType(json);
    }


    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object object) {
        return cast(object, Map.class);
    }

    public static <T> T fromJSON(String json, Class<T> type) {
        if (ObjectUtils.isEmpty(json)) {
            return null;
        }
        try {
            return getObjectMapper().readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new JSONException(e);
        }
    }

    public static <T> T fromJSON(String json, TypeReference<T> type) {
        if (ObjectUtils.isEmpty(json)) {
            return null;
        }
        try {
            return getObjectMapper().readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new JSONException(e);
        }
    }

    public static <T> T fromJsonWithType(String json) {
        if (ObjectUtils.isEmpty(json)) {
            return null;
        }
        try {
            return getObjectMapper().readValue(json, new TypeReference<T>() {
            });
        } catch (JsonProcessingException e) {
            throw new JSONException(e);
        }
    }

    public static JsonNode fromJSON(String json) {
        if (ObjectUtils.isEmpty(json)) {
            return null;
        }
        try {
            return getObjectMapper().readTree(json);
        } catch (JsonProcessingException e) {
            throw new JSONException(e);
        }
    }

    public static JsonNode toJsonNode(Object object) {
        if (object instanceof String string) {
            return fromJSON(string);
        }
        return cast(object, JsonNode.class);
    }

    public static boolean equals(Object source, Object target) {
        return Objects.equals(toJsonNode(source), toJsonNode(target));
    }

    public static class Instance {

        private final ObjectMapper objectMapper;

        private Instance(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        public ObjectMapper getObjectMapper() {
            return objectMapper;
        }

        public <T> T copy(T object) {
            return cast(object, getClass(object));
        }

        public <T> T copy(T object, TypeReference<T> typeReference) {
            return cast(object, typeReference);
        }

        @SuppressWarnings("unchecked")
        private <T> Class<T> getClass(T object) {
            return (Class<T>) object.getClass();
        }

        public <T> T cast(Object object, TypeReference<T> typeReference) {
            if (object == null) {
                return null;
            }
            return objectMapper.convertValue(object, typeReference);
        }

        public <T> T cast(Object object, Class<T> classType) {
            if (object == null) {
                return null;
            }
            return objectMapper.convertValue(object, classType);
        }

        public String toPrettyJSON(String json) {
            return toPrettyJSON(fromJSON(json));
        }

        public String toJSON(Object object) {
            if (object == null) {
                return null;
            }
            try {
                return objectMapper.writeValueAsString(object);
            } catch (JsonProcessingException e) {
                throw new JSONException(e);
            }
        }

        public String toPrettyJSON(Object object) {
            if (object == null) {
                return null;
            }
            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
            } catch (JsonProcessingException e) {
                throw new JSONException(e);
            }
        }

        public Map<String, Object> toMap(String json) {
            return fromJsonWithType(json);
        }


        @SuppressWarnings("unchecked")
        public Map<String, Object> toMap(Object object) {
            return cast(object, Map.class);
        }

        public <T> T fromJSON(String json, Class<T> type) {
            if (ObjectUtils.isEmpty(json)) {
                return null;
            }
            try {
                return objectMapper.readValue(json, type);
            } catch (JsonProcessingException e) {
                throw new JSONException(e);
            }
        }

        public <T> T fromJSON(String json, TypeReference<T> type) {
            if (ObjectUtils.isEmpty(json)) {
                return null;
            }
            try {
                return objectMapper.readValue(json, type);
            } catch (JsonProcessingException e) {
                throw new JSONException(e);
            }
        }

        public <T> T fromJsonWithType(String json) {
            if (ObjectUtils.isEmpty(json)) {
                return null;
            }
            try {
                return objectMapper.readValue(json, new TypeReference<T>() {
                });
            } catch (JsonProcessingException e) {
                throw new JSONException(e);
            }
        }

        public JsonNode fromJSON(String json) {
            if (ObjectUtils.isEmpty(json)) {
                return null;
            }
            try {
                return objectMapper.readTree(json);
            } catch (JsonProcessingException e) {
                throw new JSONException(e);
            }
        }

        public JsonNode toJsonNode(Object object) {
            if (object instanceof String string) {
                return fromJSON(string);
            }
            return cast(object, JsonNode.class);
        }

        public boolean equals(Object source, Object target) {
            return Objects.equals(toJsonNode(source), toJsonNode(target));
        }
    }

}
