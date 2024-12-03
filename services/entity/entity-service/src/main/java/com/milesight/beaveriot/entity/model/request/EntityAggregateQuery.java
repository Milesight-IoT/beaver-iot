package com.milesight.beaveriot.entity.model.request;

import com.milesight.beaveriot.entity.enums.AggregateType;
import lombok.Data;

/**
 * @author loong
 * @date 2024/10/22 14:13
 */
@Data
public class EntityAggregateQuery {

    private Long entityId;
    private AggregateType aggregateType;
    private Long startTimestamp;
    private Long endTimestamp;

}
