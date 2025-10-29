package com.milesight.beaveriot.data.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * TimeSeriesPeriodQuery class.
 *
 * @author simon
 * @date 2025/10/13
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TimeSeriesPeriodQuery extends TimeSeriesBaseQuery {
    private Long startTimestamp;

    private Long endTimestamp;

    private TimeSeriesQueryOrder order;

    private Long pageSize = 10L;

    private TimeSeriesCursor cursor;

    // NEXT: Aggregation
}
