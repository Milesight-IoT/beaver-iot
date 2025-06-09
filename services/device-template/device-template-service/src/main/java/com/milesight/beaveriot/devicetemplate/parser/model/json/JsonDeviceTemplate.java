package com.milesight.beaveriot.devicetemplate.parser.model.json;

import com.fasterxml.jackson.annotation.JsonCreator;
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
public class JsonDeviceTemplate {
    private String templateType;
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
    }

    @Data
    public static class Definition {
        private Input input;
        private List<OutputJsonObject> output;

        @Data
        public static class Input {
            private JsonType type;
            private List<InputJsonObject> properties;
        }

        @Data
        public static class InputJsonObject {
            private String key;
            private JsonType type;
            private String entityMapping;
            private boolean required;
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
