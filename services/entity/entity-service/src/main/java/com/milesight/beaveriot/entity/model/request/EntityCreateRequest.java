package com.milesight.beaveriot.entity.model.request;

import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import lombok.*;

import java.util.Map;

/**
 * The request body for creating an entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityCreateRequest {

    private String identifier;

    private AccessMod accessMod;

    private EntityType type;

    private String name;

    /**
     * The value attribute of the entity. <br>
     * Example: {"min":100,"max":600,"enum":{200:"OK",404:"NOT_FOUND"}}
     */
    private Map<String, Object> valueAttribute;

    private EntityValueType valueType;

    private Boolean visible;

    private String parentIdentifier;

}
