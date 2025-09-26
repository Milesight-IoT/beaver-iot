package com.milesight.beaveriot.context.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.context.integration.model.config.EntityConfig;
import lombok.Data;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/5/15 13:29
 **/
@Data
public class DeviceTemplateModel {
    private Map<String, Object> metadata;
    private Definition definition;
    private List<EntityConfig> initialEntities;
    private Codec codec;
    private Blueprint blueprint;

    @Getter
    public enum JsonType {
        OBJECT("object"),
        STRING("string"),
        LONG("long"),
        DOUBLE("double"),
        BOOLEAN("boolean");

        private final String typeName;

        JsonType(String typeName) {
            this.typeName = typeName;
        }

        @JsonCreator
        public static JsonType fromString(String value) {
            if (value == null) {
                throw new IllegalArgumentException("Type value cannot be null");
            }
            return Arrays.stream(values())
                    .filter(t -> t.name().equalsIgnoreCase(value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid type: " + value));
        }
    }

    @Data
    public static class Definition {
        private Input input;
        private Output output;

        @Data
        public static class Input {
            private JsonType type;
            private List<InputJsonObject> properties;
        }

        @Data
        public static class Output {
            private JsonType type;
            private List<OutputJsonObject> properties;
        }

        @Data
        public static class InputJsonObject {
            private String key;
            private JsonType type;
            private String entityMapping;
            private boolean required;
            @JsonProperty("is_device_id")
            private boolean isDeviceId;
            @JsonProperty("is_device_name")
            private boolean isDeviceName;
            private List<InputJsonObject> properties;
        }

        @Data
        public static class OutputJsonObject {
            private String key;
            private JsonType type;
            private String entityMapping;
            private Object value;
            private List<OutputJsonObject> properties;
        }
    }

    @Data
    public static class Codec {
        private String id;
        private String ref;
    }

    @Data
    public static class Blueprint {
        private String dir;
        private Map<String, Value> values;

        @Data
        public static class Value {
            public static final String TYPE_ENTITY_ID = "entity_id";
            public static final String TYPE_ENTITY_KEY = "entity_key";
            public static final String TYPE_DEVICE_ID = "device_id";
            public static final String TYPE_DEVICE_KEY = "device_key";
            private String type;
            private String identifier;
            private Object value;

            public boolean validate() {
                if (StringUtils.isEmpty(type)) {
                    return false;
                }

                if (TYPE_ENTITY_ID.equals(type) || TYPE_ENTITY_KEY.equals(type)) {
                    return !StringUtils.isEmpty(identifier);
                }

                if (TYPE_DEVICE_ID.equals(type) || TYPE_DEVICE_KEY.equals(type)) {
                    return true;
                }

                return value != null;
            }
        }

        public boolean validate() {
            if (StringUtils.isEmpty(dir)) {
                return false;
            }

            if (CollectionUtils.isEmpty(values)) {
                return false;
            }

            for (Value value : values.values()) {
                if (!value.validate()) {
                    return false;
                }
            }
            return true;
        }
    }
}
