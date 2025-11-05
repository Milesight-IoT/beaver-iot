package com.milesight.beaveriot.data.timeseries.influxdb;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.milesight.beaveriot.data.api.TimeSeriesRepository;
import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.model.*;
import com.milesight.beaveriot.data.support.TimeSeriesDataConverter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;

/**
 * InfluxDbTimeSeriesRepository class.
 *
 * @author simon
 * @date 2025/10/10
 */
@Slf4j
public class InfluxDbTimeSeriesRepository<T> implements TimeSeriesRepository<T> {
    @Resource
    private InfluxDbClient client;
    private final String bucket;
    private final String tableName;
    private final String timeColumn;
    private final Set<String> indexedColumns;
    private final TimeSeriesDataConverter converter;
    private final Class<T> poClass;

    public InfluxDbTimeSeriesRepository(
            String bucket,
            String tableName,
            String timeColumn,
            List<String> indexedColumns,
            TimeSeriesDataConverter converter,
            Class<T> poClass
    ) {
        this.bucket = bucket;
        this.tableName = tableName;
        this.timeColumn = timeColumn;
        this.indexedColumns = new TreeSet<>(indexedColumns);
        this.converter = converter;
        this.poClass = poClass;
    }

    @Override
    public TimeSeriesResult<T> findByTimePoints(TimeSeriesTimePointQuery query) {
        if (query.getTimestampList() == null || query.getTimestampList().isEmpty()) {
            return TimeSeriesResult.of();
        }

        query.validate(indexedColumns);

        Consumer<Filterable> timePointFilterable = f -> f.in(InfluxDbConstants.TIME_COLUMN, query.getTimestampList().toArray(new Long[0]));
        Consumer<Filterable> filterable = query.getFilterable() == null ? timePointFilterable : query.getFilterable().andThen(timePointFilterable);
        long currMillis = System.currentTimeMillis();
        List<FluxTable> queryRes = this.client.getQueryApi().query(new FluxQueryBuilder(bucket, tableName)
                .indexedColumns(indexedColumns)
                .indexedKeyValues(query.getIndexedKeyValues())
                .start(query.getTimestampList().stream().min(Long::compare).orElseGet(() -> currMillis - (365L * 24 * 60 * 60 * 1000)))
                .end(query.getTimestampList().stream().max(Long::compare).orElse(currMillis) + 1)
                .filter(filterable)
                .build());
        return convertToPOResult(queryRes);
    }

    @Override
    public TimeSeriesResult<T> findByPeriod(TimeSeriesPeriodQuery query) {
        TimeSeriesCursor cursor = query.getCursor();
        Long pageSize = query.getPageSize();
        TimeSeriesQueryOrder order = query.getOrder();

        Long start;
        Long end;
        if (cursor == null) {
            start = query.getStartTimestamp();
            end = query.getEndTimestamp();
        } else {
            if (order == TimeSeriesQueryOrder.ASC) {
                start = cursor.getTimestamp();
                end = query.getEndTimestamp();
            } else {
                start = query.getStartTimestamp();
                end = cursor.getTimestamp() + 1;
            }
        }

        query.validate(indexedColumns);

        FluxQueryBuilder queryBuilder = new FluxQueryBuilder(bucket, tableName)
                .indexedColumns(indexedColumns)
                .indexedKeyValues(query.getIndexedKeyValues())
                .start(start)
                .end(end)
                .filter(query.getFilterable())
                .cursor(cursor)
                .limit(Math.toIntExact(pageSize + 1))
                .order(order);

        List<FluxTable> tables = this.client.getQueryApi().query(queryBuilder.build());

        List<T> result = convertToPOList(tables);

        TimeSeriesCursor nextCursor = null;

        if (result.size() > pageSize) {
            T lastItem = result.get(Math.toIntExact(pageSize));
            Map<String, Object> map = converter.toMap(lastItem);
            Long lastTime = (Long) map.get(timeColumn);

            TimeSeriesCursor.Builder cursorBuilder = new TimeSeriesCursor.Builder(lastTime);
            for (String column : indexedColumns) {
                cursorBuilder.putIndexedKeyValues(column, map.get(column));
            }
            nextCursor = cursorBuilder.build();

            result = result.subList(0, Math.toIntExact(pageSize));
        }
        return TimeSeriesResult.of(result, nextCursor);
    }

    @Override
    public void save(List<T> itemList) {
        this.client.getWriteApi().writePoints(bucket, client.getOrgName(), itemList.stream().map(po -> {
            Point dataPoint = Point.measurement(tableName);
            this.converter.toMap(po).forEach((k, v) -> {
                if (timeColumn.equals(k)) {
                    dataPoint.time((Long) v, WritePrecision.MS);
                } else if (indexedColumns.contains(k)) {
                    dataPoint.addTag(k, v.toString());
                } else {
                    if (v instanceof String vStr) {
                        dataPoint.addField(k, vStr);
                    } else if (v instanceof Long vLong) {
                        dataPoint.addField(k, vLong);
                    } else if (v instanceof Double vDouble) {
                        dataPoint.addField(k, vDouble);
                    } else if (v instanceof Boolean vBoolean) {
                        dataPoint.addField(k, vBoolean);
                    } else if (v != null) {
                        // TODO: not supported bytes, considering remove this entity value type?
                        throw new IllegalArgumentException("Invalid influxdb data type: " + v.getClass().getName());
                    }
                }
            });

            return dataPoint;
        }).toList());
    }

    private TimeSeriesResult<T> convertToPOResult(List<FluxTable> tables) {
        return TimeSeriesResult.of(convertToPOList(tables));
    }

    private List<T> convertToPOList(List<FluxTable> tables) {
        Map<String, Map<String, Object>> groupMap = new LinkedHashMap<>();

        for (FluxTable table : tables) {
            for (FluxRecord rec : table.getRecords()) {
                if (rec.getTime() == null) {
                    continue;
                }

                Long time = rec.getTime().toEpochMilli();

                StringBuilder key = new StringBuilder(time.toString());
                for (String column : indexedColumns) {
                    Object columnValue = rec.getValueByKey(column);
                    key.append("|").append((String) columnValue);
                }

                Map<String, Object> map = groupMap.computeIfAbsent(key.toString(), k -> {
                    Map<String, Object> nMap = new HashMap<>();
                    nMap.put(timeColumn, time);
                    for (String column : indexedColumns) {
                        nMap.put(column, rec.getValueByKey(column));
                    }

                    return nMap;
                });

                map.put((String) rec.getValueByKey("_field"), rec.getValueByKey("_value"));
            }
        }
        return groupMap.values().stream().map(m -> converter.fromMap(m, poClass)).toList();
    }
}
