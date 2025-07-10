package com.milesight.beaveriot.data.jpa.repository;

import com.milesight.beaveriot.data.api.BaseRepository;
import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.jpa.support.SpecificationConverter;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author leon
 */
@NoRepositoryBean
public interface BaseJpaRepository<T,ID extends Serializable> extends JpaRepository<T,ID>, BaseRepository<T,ID>, JpaSpecificationExecutor<T> {

    @Override
    T getOne(ID id);

    @Override
    <S extends T> S save(S entity);

    @Override
    Optional<T> findById(ID id);

    @Override
    void deleteById(ID id);

    @Override
    void delete(T entity);

    @Override
    void deleteAllById(Iterable<? extends ID> ids);

    @Override
    <S extends T> List<S> saveAll(Iterable<S> entities);

    @Override
    List<T> findAll();

    @Override
    List<T> findAll(Sort sort);

    @Override
    Page<T> findAll(Pageable pageable);


    @Override
    default <S extends T> List<T> findBy(Consumer<Filterable> consumer, Function<FluentQuery.FetchableFluentQuery<S>, List<T>> queryFunction){
        return findBy(SpecificationConverter.toSpecification(consumer), queryFunction);
    }

    @Override
    default List<T> findAll(Consumer<Filterable> consumer){
        return findAll(SpecificationConverter.toSpecification(consumer));
    }

    @Override
    default List<T> findAll(Consumer<Filterable> consumer, Sort sort){
        return findAll(SpecificationConverter.toSpecification(consumer), sort);
    }

    @Override
    default Page<T> findAll(Consumer<Filterable> filterable, Pageable pageable){
        return findAll(SpecificationConverter.toSpecification(filterable), pageable);
    }

    @Override
    default T findUniqueOne(Consumer<Filterable> filterable){
        return findOne(filterable).orElseThrow(() -> new EmptyResultDataAccessException(1));
    }

    @Override
    default Optional<T> findOne(Consumer<Filterable> filterable){
        List<T> all = findAll(SpecificationConverter.toSpecification(filterable));
        return CollectionUtils.isEmpty(all) ? Optional.empty() : Optional.of(all.get(0));
    }

    @Override
    default Long count(Consumer<Filterable> filterable){
        return count(SpecificationConverter.toSpecification(filterable));
    }
}
