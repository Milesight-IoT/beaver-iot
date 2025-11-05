package com.milesight.beaveriot.data.timeseries.jpa;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.data.api.TimeSeriesRepository;
import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.data.model.TimeSeriesCursor;
import com.milesight.beaveriot.data.model.TimeSeriesPeriodQuery;
import com.milesight.beaveriot.data.model.TimeSeriesQueryOrder;
import com.milesight.beaveriot.data.model.TimeSeriesResult;
import com.milesight.beaveriot.data.model.TimeSeriesTimePointQuery;
import com.milesight.beaveriot.data.support.TimeSeriesDataConverter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * JpaTimeSeriesRepository class.
 *
 * @author simon
 * @date 2025/10/11
 */
public class JpaTimeSeriesRepository<T> implements TimeSeriesRepository<T> {
    @Resource
    ApplicationContext applicationContext;
    private BaseJpaRepository<T, ?> jpaRepository;
    @Getter
    private final Class<T> entityClass;
    private final String timeColumn;
    private final List<String> indexedColumns;
    private final TimeSeriesDataConverter converter;

    public JpaTimeSeriesRepository(
            Class<T> entityClass,
            String timeColumn,
            List<String> indexedColumns,
            TimeSeriesDataConverter converter
    ) {
        this.entityClass = entityClass;
        this.timeColumn = timeColumn;
        this.indexedColumns = indexedColumns;
        this.converter = converter;
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    private void initJpaRepo() {
        String beanName = applicationContext.getBeanNamesForType(ResolvableType.forClassWithGenerics(BaseJpaRepository.class, entityClass, Long.class))[0];
        jpaRepository = (BaseJpaRepository<T, ?>) applicationContext.getBean(beanName);
    }

    @Override
    public TimeSeriesResult<T> findByTimePoints(TimeSeriesTimePointQuery query) {
        TimeSeriesResult<T> result = new TimeSeriesResult<>();
        if (ObjectUtils.isEmpty(query.getTimestampList())) {
            return result;
        }

        query.validate(indexedColumns);

        Consumer<Filterable> filterable = toIndexFilterable(query.getIndexedKeyValues());
        filterable = filterable.andThen(f -> f.in(timeColumn, query.getTimestampList().toArray(new Long[0])));
        if (query.getFilterable() != null) {
            filterable = filterable.andThen(query.getFilterable());
        }

        result.setContent(jpaRepository.findAll(filterable));
        return result;
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
        Consumer<Filterable> filterable = toIndexFilterable(query.getIndexedKeyValues());
        Consumer<Filterable> timeFilterable = fe -> fe.ge(timeColumn, start).lt(timeColumn, end);
        filterable = filterable.andThen(timeFilterable);

        filterable = query.getFilterable() == null ? filterable : filterable.andThen(query.getFilterable());
        if (cursor != null && !cursor.getIndexedKeyValues().isEmpty()) {
            filterable = filterable.andThen(getSortKeyFilterable(cursor));
        }

        List<Sort.Order> orders = new ArrayList<>();
        Sort.Order timeOrder = TimeSeriesQueryOrder.DESC.equals(order) ? Sort.Order.desc(this.timeColumn) : Sort.Order.asc(this.timeColumn);
        orders.add(timeOrder);
        if (!CollectionUtils.isEmpty(indexedColumns)) {
            indexedColumns.forEach(indexedColumn -> orders.add(Sort.Order.asc(indexedColumn)));
        }
        Sort sort = Sort.by(orders);

        List<T> result = jpaRepository.findAll(filterable, PageRequest.of(
                0,
                Math.toIntExact(query.getPageSize() + 1),
                sort
        )).stream().toList();

        TimeSeriesCursor nextCursor = null;

        if (result.size() > query.getPageSize()) {
            T lastItem = result.get(Math.toIntExact(pageSize));
            Map<String, Object> map = toLowerCamelCaseKeys(converter.toMap(lastItem));
            Long lastTime = (Long) map.get(timeColumn);

            TimeSeriesCursor.Builder cursorBuilder = new TimeSeriesCursor.Builder(lastTime);
            for (String column : indexedColumns) {
                cursorBuilder.putIndexedKeyValues(StringUtils.toSnakeCase(column), map.get(column));
            }
            nextCursor = cursorBuilder.build();

            result = result.subList(0, Math.toIntExact(pageSize));
        }

        return TimeSeriesResult.of(result, nextCursor);
    }

    @Override
    public void save(List<T> ditemList) {
        jpaRepository.saveAll(ditemList);
    }

    public static Map<String, Object> toLowerCamelCaseKeys(Map<String, Object> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String camelKey = StringUtils.toCamelCase(entry.getKey());
            result.put(camelKey, entry.getValue());
        }
        return result;
    }

    private Consumer<Filterable> getSortKeyFilterable(TimeSeriesCursor cursor) {
        Map<String, Object> sortKeyValues = cursor.getIndexedKeyValues();
        return f1 -> f1.and(f2 -> sortKeyValues.forEach((key, value) -> f2.ge(StringUtils.toCamelCase(key), value.toString())));
    }

    private Consumer<Filterable> toIndexFilterable(Map<String, Object> indexedKeyValues) {
        return f1 -> f1.and(f2 -> indexedKeyValues.forEach((key, value) -> f2.eq(StringUtils.toCamelCase(key), value)));
    }

    /**
     * Delete a single batch of records before the specified timestamp in a new transaction.
     * Using REQUIRES_NEW to ensure each batch is in its own independent transaction.
     *
     * @param timestamp the timestamp before which data should be deleted
     * @param limit the limit size for this deletion operation
     * @return the number of deleted records in this batch
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteByTimeBefore(long timestamp, int limit) {
        Consumer<Filterable> filterable = fe -> fe.lt(timeColumn, timestamp);
        List<T> toDelete = jpaRepository.findAll(filterable, PageRequest.of(0, limit)).stream().toList();

        if (!toDelete.isEmpty()) {
            jpaRepository.deleteAll(toDelete);
        }

        return toDelete.size();
    }

}
