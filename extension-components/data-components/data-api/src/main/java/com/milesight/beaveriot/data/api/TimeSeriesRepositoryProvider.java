package com.milesight.beaveriot.data.api;

/**
 * TimeSeriesRepositoryProvider
 *
 * @author simon
 * @date 2025/10/11
 */
public interface TimeSeriesRepositoryProvider {
    <T> TimeSeriesRepository<T> get(Class<T> entityClass);
}
