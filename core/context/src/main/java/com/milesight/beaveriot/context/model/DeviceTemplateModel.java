package com.milesight.beaveriot.context.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.milesight.beaveriot.context.integration.model.config.EntityConfig;
import lombok.Data;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * author: Luxb
 * create: 2025/5/15 13:29
 **/
@Data
public class DeviceTemplateModel {
    private Definition definition;
    private List<EntityConfig> initialEntities;

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

        @JsonValue
        public String getTypeName() {
            return typeName;
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
            @JsonProperty("enum")
            private List<String> enumValues;
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
}
