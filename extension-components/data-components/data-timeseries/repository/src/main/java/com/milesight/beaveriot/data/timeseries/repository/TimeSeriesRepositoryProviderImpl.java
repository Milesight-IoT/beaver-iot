package com.milesight.beaveriot.data.timeseries.repository;

import com.milesight.beaveriot.data.api.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TimeSeriesRepositoryProviderImpl class.
 *
 * @author simon
 * @date 2025/10/11
 */
@Component
@Slf4j
public class TimeSeriesRepositoryProviderImpl implements TimeSeriesRepositoryProvider {
    @Autowired
    private TimeSeriesConfiguration timeSeriesConfiguration;

    @Override
    public <T> TimeSeriesRepository<T> get(Class<T> entityClass) {
        TimeSeriesRepository<T> repo = (TimeSeriesRepository<T>) timeSeriesConfiguration.getRepos().get(entityClass);
        if (repo == null) {
            throw new IllegalArgumentException("TimeSeries Repository not found for " + entityClass.getName() + ". Please check if your jpa repository is annotated by @SupportTimeSeries");
        }
        return repo;
    }
}
