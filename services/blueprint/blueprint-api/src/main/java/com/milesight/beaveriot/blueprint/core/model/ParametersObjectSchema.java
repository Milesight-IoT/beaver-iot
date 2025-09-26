package com.milesight.beaveriot.blueprint.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.milesight.beaveriot.base.utils.JsonUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
@FieldNameConstants
public class ParametersObjectSchema {

    private String type = "object";

    private JsonNode properties;

    private List<String> required;

    public ParametersObjectSchema(JsonNode properties) {
        this.properties = JsonUtils.copy(properties);
        this.required = new ArrayList<>();
        this.properties.properties().forEach(prop -> {
            if (prop.getValue() instanceof ObjectNode obj
                    && (obj.get(Fields.required) instanceof BooleanNode isRequired)) {
                if (isRequired.booleanValue()) {
                    this.required.add(prop.getKey());
                }
                obj.remove(Fields.required);
            }
        });
        if (this.required.isEmpty()) {
            this.required = null;
        }
    }

}
