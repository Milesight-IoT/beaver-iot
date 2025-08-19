package com.milesight.beaveriot.entity.model.request;

import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.entity.constants.EntityDataFieldConstants;
import jakarta.validation.constraints.Size;
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

    @Size(max = EntityDataFieldConstants.CUSTOM_ENTITY_IDENTIFIER_MAX_LENGTH)
    private String identifier;

    private AccessMod accessMod;

    private EntityType type;

    @Size(max = EntityDataFieldConstants.ENTITY_NAME_MAX_LENGTH)
    private String name;

    /**
     * The value attribute of the entity. <br>
     * Example: {"min":100,"max":600,"unit":"","enum":{200:"OK",404:"NOT_FOUND"}}
     */
    private Map<String, Object> valueAttribute;

    private EntityValueType valueType;

    private Boolean visible;
}
