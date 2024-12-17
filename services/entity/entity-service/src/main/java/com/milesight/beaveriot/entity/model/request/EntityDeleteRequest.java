package com.milesight.beaveriot.entity.model.request;

import lombok.*;

import java.util.List;

/**
 * The request body for deleting entities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityDeleteRequest {

    /**
     * The IDs of the entities to be deleted.
     */
    private List<Long> entityIds;

}
