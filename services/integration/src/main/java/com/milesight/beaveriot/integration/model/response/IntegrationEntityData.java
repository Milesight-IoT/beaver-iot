package com.milesight.beaveriot.integration.model.response;

import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Builder
@Data
public class IntegrationEntityData {
    private String id;
    private String key;
    private String name;
    private EntityType type;
    private Map<String, Object> valueAttribute;
    private EntityValueType valueType;
    private AccessMod accessMod;
    private String parent;
    private Object value;
    private String description;
}
