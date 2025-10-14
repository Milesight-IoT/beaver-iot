package com.milesight.beaveriot.data.timeseries.repository;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.data.api.SupportTimeSeries;
import com.milesight.beaveriot.data.api.TimeSeriesRepository;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.data.support.TimeSeriesDataConverter;
import com.milesight.beaveriot.data.timeseries.common.TimeSeriesProperty;
import com.milesight.beaveriot.data.timeseries.influxdb.InfluxDbClient;
import com.milesight.beaveriot.data.timeseries.influxdb.InfluxDbTimeSeriesRepository;
import com.milesight.beaveriot.data.timeseries.jpa.JpaTimeSeriesRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TimeSeriesConfiguration class.
 *
 * @author simon
 * @date 2025/10/11
 */
@Slf4j
@Component
public class TimeSeriesConfiguration implements SmartInitializingSingleton {
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private EntityManager entityManager;

    @Autowired(required = false)
    private InfluxDbClient influxDbClient;

    @Autowired
    private TimeSeriesProperty timeSeriesProperty;

    @Getter
    private final Map<Class<?>, TimeSeriesRepository<?>> repos = new HashMap<>();

    private SupportTimeSeries getAnnotation(Object repo) {
        for (Class<?> repoInterface : repo.getClass().getInterfaces()) {
            SupportTimeSeries[] annotations = repoInterface.getAnnotationsByType(SupportTimeSeries.class);
            if (annotations.length > 0) {
                return annotations[0];
            }
        }

        return null;
    }

    private String getTableName(SupportTimeSeries supportTimeSeries) {
        // use specified table name first
        if (!supportTimeSeries.tableName().isEmpty()) {
            return supportTimeSeries.tableName();
        }

        // auto find table name from entity po
        Class<?> entityClass = supportTimeSeries.entity();
        Table[] annotations = entityClass.getAnnotationsByType(Table.class);
        if (annotations.length == 0) {
            throw new IllegalArgumentException(entityClass.getName() + " cannot find @Table to get table name");
        }

        String tableName = annotations[0].name();
        if (ObjectUtils.isEmpty(tableName)) {
            throw new IllegalArgumentException(entityClass.getName() + " has an invalid table name");
        }

        return tableName;
    }

    private TimeSeriesDataConverter createConverterInstance(SupportTimeSeries supportTimeSeries) {
        try {
            Constructor<?> constructor = supportTimeSeries.converter().getConstructor();
            return (TimeSeriesDataConverter) constructor.newInstance();
        } catch (Exception e) {
           throw new IllegalArgumentException("Illegal converter class");
        }
    }

    private String getTimeColumnName(SupportTimeSeries supportTimeSeries) {
        return StringUtils.toSnakeCase(supportTimeSeries.timeColumn());
    }

    private List<String> getIndexedColumnNameList(SupportTimeSeries supportTimeSeries) {
        return Arrays.stream(supportTimeSeries.indexedColumns()).map(StringUtils::toSnakeCase).toList();
    }

    @Override
    public void afterSingletonsInstantiated() {
        applicationContext
                .getBeansOfType(BaseJpaRepository.class)
                .values()
                .forEach(jpaRepo -> {
                    SupportTimeSeries supportTimeSeries = getAnnotation(jpaRepo);
                    if (supportTimeSeries != null) {
                        String tableName = getTableName(supportTimeSeries);
                        log.debug("loading time series table: " + tableName);
                        TimeSeriesRepository<?> repository;
                        if ("influxdb".equals(timeSeriesProperty.getDatabase())) {
                            repository = new InfluxDbTimeSeriesRepository<>(
                                    influxDbClient,
                                    supportTimeSeries.category(),
                                    tableName,
                                    getTimeColumnName(supportTimeSeries),
                                    getIndexedColumnNameList(supportTimeSeries),
                                    createConverterInstance(supportTimeSeries),
                                    supportTimeSeries.entity()
                            );
                        } else {
                            repository = new JpaTimeSeriesRepository<>(jpaRepo, getTimeColumnName(supportTimeSeries));
                        }

                        repos.put(supportTimeSeries.entity(), repository);
                    }
                });
    }
}
