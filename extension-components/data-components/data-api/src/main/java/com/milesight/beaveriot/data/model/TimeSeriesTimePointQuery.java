package com.milesight.beaveriot.data.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * TimeSeriesTimePointQuery class.
 *
 * @author simon
 * @date 2025/10/13
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TimeSeriesTimePointQuery extends TimeSeriesBaseQuery {
    private List<Long> timestampList;
}
