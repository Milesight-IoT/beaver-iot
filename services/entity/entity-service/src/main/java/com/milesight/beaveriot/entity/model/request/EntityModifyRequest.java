package com.milesight.beaveriot.entity.model.request;

import lombok.*;

/**
 * The request body for modifying an entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityModifyRequest {

    private String name;

}
