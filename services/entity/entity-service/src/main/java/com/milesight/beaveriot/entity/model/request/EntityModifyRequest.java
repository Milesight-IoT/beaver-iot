package com.milesight.beaveriot.entity.model.request;

import com.milesight.beaveriot.entity.constants.EntityDataFieldConstants;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Map;

/**
 * The request body for modifying an entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityModifyRequest {

    @Size(max = EntityDataFieldConstants.ENTITY_NAME_MAX_LENGTH)
    private String name;
    private Map<String, Object> valueAttribute;

}
