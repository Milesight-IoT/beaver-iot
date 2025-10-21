package com.milesight.beaveriot.data.model;

import lombok.Data;

import java.util.List;

/**
 * TimeSeriesPeriodQuery class.
 *
 * @author simon
 * @date 2025/10/13
 */
@Data
public class TimeSeriesPeriodQuery extends TimeSeriesBaseQuery {
    private Long startTimestamp;

    private Long endTimestamp;

    private TimeSeriesQueryOrder order;

    private Long pageNumber = 0L;

    private Long pageSize = 10L;

    // NEXT: Aggregation
}
