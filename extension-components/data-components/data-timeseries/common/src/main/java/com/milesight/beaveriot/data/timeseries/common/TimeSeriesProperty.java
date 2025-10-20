package com.milesight.beaveriot.data.timeseries.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * TimeSeriesProperty class.
 *
 * @author simon
 * @date 2025/10/16
 */
@ConfigurationProperties(prefix = "timeseries")
@Data
@Component
public class TimeSeriesProperty {
    private String database;

    private Map<String, Duration> retention;
}
