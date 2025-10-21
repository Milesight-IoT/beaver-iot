package com.milesight.beaveriot.data.api;

import com.milesight.beaveriot.data.model.TimeSeriesPeriodQuery;
import com.milesight.beaveriot.data.model.TimeSeriesResult;
import com.milesight.beaveriot.data.model.TimeSeriesTimePointQuery;

import java.util.List;

/**
 * TimeSeriesRepository
 *
 * @author simon
 * @date 2025/10/10
 */
public interface TimeSeriesRepository<T> {
    TimeSeriesResult<T> findByTimePoints(TimeSeriesTimePointQuery timePointQueries);

    TimeSeriesResult<T> findByPeriod(TimeSeriesPeriodQuery query);

    void save(List<T> itemList);
}
