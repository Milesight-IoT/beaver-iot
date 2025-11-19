package com.milesight.beaveriot.data.timeseries.jpa;

import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.data.api.SupportTimeSeries;
import com.milesight.beaveriot.data.api.TimeSeriesRepository;
import com.milesight.beaveriot.data.timeseries.common.TimeSeriesProperty;
import com.milesight.beaveriot.permission.helper.TenantValidationBypass;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.Table;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Scheduled task for cleaning up expired time-series data in JPA repositories.
 * This scheduler only runs when 'timeseries.database' is set to "jpa" and cleanup is enabled.
 */
@Slf4j
@RequiredArgsConstructor
@Component
@EnableScheduling
@ConditionalOnProperty(name = "timeseries.database", havingValue = "jpa")
public class JpaTimeSeriesDataCleanupScheduler {

    private final TimeSeriesProperty timeSeriesProperty;
    private final List<TimeSeriesRepository<?>> repositories;
    private final Map<String, Collection<String>> categoryToTableNamesMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // Build category to table names mapping from all repositories
        buildCategoryToTableNamesMap();

        if (timeSeriesProperty.getCleanup().isEnabled()) {
            log.info("JPA time-series data cleanup scheduler initialized with cron: {}",
                    timeSeriesProperty.getCleanup().getCron());
            log.info("Retention policies: {}", timeSeriesProperty.getRetention());
            log.info("Category to table mapping: {}", categoryToTableNamesMap);
        } else {
            log.info("JPA time-series data cleanup scheduler is disabled");
        }
    }

    /**
     * Build mapping from TimeSeriesCategory to actual table names.
     * This allows configuration to use category names (e.g., "beaver_iot_telemetry")
     * instead of database-specific table names (e.g., "t_entity_history").
     * A category can map to multiple tables.
     */
    private void buildCategoryToTableNamesMap() {
        for (TimeSeriesRepository<?> timeSeriesRepository : repositories) {
            if (!(timeSeriesRepository instanceof JpaTimeSeriesRepository<?> repository)) {
                return;
            }

            Class<?> entityClass = repository.getEntityClass();

            // Get category from @SupportTimeSeries annotation
            SupportTimeSeries annotation = entityClass.getAnnotation(SupportTimeSeries.class);
            if (annotation != null) {
                String category = annotation.category();
                String tableName = getTableNameFromRepository(repository);

                // Add table name to the list for this category
                categoryToTableNamesMap.computeIfAbsent(category, k -> new HashSet<>()).add(tableName);
                log.debug("Mapped category '{}' to table '{}'", category, tableName);
            }
        }
    }

    /**
     * Scheduled task to clean up expired time-series data.
     * Runs based on the cron expression configured in timeseries.cleanup.cron.
     * Note: No @Transactional here - each batch deletion will be in its own transaction.
     */
    @Scheduled(cron = "${timeseries.cleanup.cron:0 0 2 * * ?}")
    @DistributedLock(name = "jpa-timeseries-cleanup", lockAtLeastFor = "59s", lockAtMostFor = "59s", scope = LockScope.GLOBAL, throwOnLockFailure = false)
    public void cleanupExpiredData() {
        if (!timeSeriesProperty.getCleanup().isEnabled()) {
            return;
        }

        Map<String, Duration> retentionPolicies = timeSeriesProperty.getRetention();
        if (retentionPolicies == null || retentionPolicies.isEmpty()) {
            log.debug("No retention policies configured, skipping cleanup");
            return;
        }

        log.info("Starting time-series data cleanup task...");

        TenantValidationBypass.run(() -> {
            long startTime = System.currentTimeMillis();
            int totalDeleted = 0;
            
            for (Map.Entry<String, Duration> entry : retentionPolicies.entrySet()) {
                String category = entry.getKey();
                Duration retention = entry.getValue();

                try {
                    int deleted = cleanupByCategory(category, retention);
                    totalDeleted += deleted;

                    if (timeSeriesProperty.getCleanup().isLogStatistics() && deleted > 0) {
                        log.info("Cleaned up {} records from category '{}' with retention policy {}",
                                deleted, category, retention);
                    }
                } catch (Exception e) {
                    log.error("Failed to cleanup category '{}': {}", category, e.getMessage(), e);
                }
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            if (timeSeriesProperty.getCleanup().isLogStatistics()) {
                log.info("Time-series data cleanup completed. Total deleted: {}, Time elapsed: {} ms",
                        totalDeleted, elapsed);
            }
        });
    }

    /**
     * Clean up expired data for a specific time-series category.
     *
     * @param category  the category name from retention configuration (e.g., "beaver_iot_telemetry")
     * @param retention the retention duration
     * @return the number of deleted records
     */
    private int cleanupByCategory(String category, Duration retention) {
        // Resolve category to actual table names (can be multiple tables)
        Collection<String> tableNames = categoryToTableNamesMap.get(category);

        if (tableNames == null || tableNames.isEmpty()) {
            log.warn("No table mapping found for category '{}', skipping cleanup", category);
            return 0;
        }

        long expirationTimestamp = Instant.now().minus(retention).toEpochMilli();
        int batchSize = timeSeriesProperty.getCleanup().getBatchSize();
        int totalDeleted = 0;

        // Clean up all tables associated with this category
        for (String tableName : tableNames) {
            JpaTimeSeriesRepository<?> repository = findRepositoryByTableName(tableName);

            if (repository == null) {
                log.warn("No repository found for table '{}' (category: '{}'), skipping cleanup",
                        tableName, category);
                continue;
            }

            try {
                int deleted = 0;
                int deletedInBatch;

                do {
                    // Each batch deletion runs in its own transaction
                    deletedInBatch = repository.deleteByTimeBefore(expirationTimestamp, batchSize);
                    deleted += deletedInBatch;
                } while (deletedInBatch == batchSize);

                totalDeleted += deleted;
                log.debug("Deleted {} records from category '{}' (table: '{}') using batch size {}",
                        deleted, category, tableName, batchSize);
            } catch (Exception e) {
                log.error("Error deleting expired data from category '{}' (table: '{}'): {}",
                        category, tableName, e.getMessage(), e);
                throw e;
            }
        }

        return totalDeleted;
    }

    /**
     * Find the JpaTimeSeriesRepository associated with the given table name.
     *
     * @param tableName the actual database table name
     * @return the repository, or null if not found
     */
    private JpaTimeSeriesRepository<?> findRepositoryByTableName(String tableName) {
        for (TimeSeriesRepository<?> timeSeriesRepository : repositories) {
            if (!(timeSeriesRepository instanceof JpaTimeSeriesRepository<?> repo)) {
                continue;
            }

            String repoTableName = getTableNameFromRepository(repo);

            if (tableName.equals(repoTableName)) {
                return repo;
            }
        }

        log.debug("No repository found for table name: {}", tableName);
        return null;
    }

    /**
     * Extract table name from repository's entity class.
     *
     * @param repository the JPA time series repository
     * @return the table name
     */
    private String getTableNameFromRepository(JpaTimeSeriesRepository<?> repository) {
        Class<?> entityClass = repository.getEntityClass();

        // Try to get from @Table annotation
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation == null || tableAnnotation.name().isEmpty()) {
            throw new IllegalArgumentException("Cannot find table name for entity class: " + entityClass.getName());
        }

        return tableAnnotation.name();
    }

}
