package com.milesight.beaveriot.data.timeseries.jpa;

import com.milesight.beaveriot.data.api.TimeSeriesRepository;
import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.data.model.*;
import com.milesight.beaveriot.data.support.TimeSeriesDataConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
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
    @Autowired
    ApplicationContext applicationContext;
    private BaseJpaRepository<T, ?> jpaRepository;
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

        Consumer<Filterable> filterable = query.getFilterable().andThen(fe -> fe.in(timeColumn, query.getTimestampList().toArray(new Long[0])));
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
                end = cursor.getTimestamp();
            }
        }

        Consumer<Filterable> timeFilterable = fe -> fe.ge(timeColumn, start).lt(timeColumn, end);
        Consumer<Filterable> filterable = query.getFilterable() == null ? timeFilterable : query.getFilterable().andThen(timeFilterable);
        if (cursor != null && !cursor.getSortKeyValues().isEmpty()) {
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
            T lastItem = result.get(result.size() - 1);
            Map<String, Object> map = converter.toMap(lastItem);
            Long lastTime = (Long) map.get(timeColumn);

            TimeSeriesCursor.Builder cursorBuilder = new TimeSeriesCursor.Builder(lastTime);
            for (String column : indexedColumns) {
                cursorBuilder.putSortKeyValue(column, map.get(column));
            }
            nextCursor = cursorBuilder.build();

            result = result.subList(0, Math.toIntExact(pageSize));
        }

        return TimeSeriesResult.of(result, nextCursor);
    }

    private Consumer<Filterable> getSortKeyFilterable(TimeSeriesCursor cursor) {
        Map<String, Object> sortKeyValues = cursor.getSortKeyValues();
        return f1 -> f1.and(f2 -> sortKeyValues.forEach((key, value) -> f2.ge(key, value.toString())));
    }

    @Override
    public void save(List<T> ditemList) {
        jpaRepository.saveAll(ditemList);
    }
}
