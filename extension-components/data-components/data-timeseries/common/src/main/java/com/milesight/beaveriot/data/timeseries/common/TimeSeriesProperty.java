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
    public static final String TIMESERIES_DATABASE = "timeseries.database";

    private String database;

    private Map<String, Duration> retention;

    private CleanupConfig cleanup = new CleanupConfig();

    @Data
    public static class CleanupConfig {
        /**
         * Whether to enable automatic cleanup of expired time-series data.
         * Default: true
         */
        private boolean enabled = true;

        /**
         * Cron expression for scheduling cleanup task.
         * Default: "0 0 2 * * ?" (run at 2:00 AM every day)
         */
        private String cron = "0 0 2 * * ?";

        /**
         * Batch size for deletion to avoid long transactions.
         * Default: 1000
         */
        private int batchSize = 1000;

        /**
         * Whether to log cleanup statistics.
         * Default: true
         */
        private boolean logStatistics = true;
    }
}
