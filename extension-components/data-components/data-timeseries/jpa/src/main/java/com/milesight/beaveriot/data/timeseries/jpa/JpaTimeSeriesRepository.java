package com.milesight.beaveriot.data.timeseries.jpa;

import com.milesight.beaveriot.data.api.TimeSeriesRepository;
import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.data.model.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.ObjectUtils;

import java.util.List;
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

    public JpaTimeSeriesRepository(
            Class<T> entityClass,
            String timeColumn
    ) {
        this.entityClass = entityClass;
        this.timeColumn = timeColumn;
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
        TimeSeriesResult<T> result = new TimeSeriesResult<>();
        Consumer<Filterable> timeFilterable = fe -> fe.ge(timeColumn, query.getStartTimestamp()).lt(timeColumn, query.getEndTimestamp());
        Consumer<Filterable> filterable = query.getFilterable() == null ? timeFilterable : query.getFilterable().andThen(timeFilterable);
        result.setContent(jpaRepository.findAll(filterable, PageRequest.of(
                Math.toIntExact(query.getPageNumber()),
                Math.toIntExact(query.getPageSize()),
                TimeSeriesQueryOrder.DESC.equals(query.getOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                this.timeColumn
        )).stream().toList());
        return result;
    }

    @Override
    public void save(List<T> ditemList) {
        jpaRepository.saveAll(ditemList);
    }
}
