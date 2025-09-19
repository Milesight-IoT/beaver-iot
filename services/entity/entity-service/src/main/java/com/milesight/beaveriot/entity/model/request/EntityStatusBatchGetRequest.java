package com.milesight.beaveriot.entity.model.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * EntityStatusBatchGetRequest class.
 *
 * @author simon
 * @date 2025/9/19
 */
@Data
public class EntityStatusBatchGetRequest {
    @Size(max = 5000)
    private List<Long> entityIds;
}
