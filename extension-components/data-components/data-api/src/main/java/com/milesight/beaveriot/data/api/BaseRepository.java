package com.milesight.beaveriot.data.api;

import com.milesight.beaveriot.data.filterable.Filterable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author leon
 */
public interface BaseRepository<T,ID extends Serializable> {
    T getOne(ID id);

    <S extends T> S save(S entity);

    <S extends T> List<S> saveAll(Iterable<S> entities);

    Optional<T> findById(ID id);

    void deleteById(ID id);

    void delete(T entity);

    void deleteAllById(Iterable<? extends ID> ids);

    List<T> findAll();

    List<T> findAll(Sort sort);

    Page<T> findAll(Pageable pageable);

    <S extends T> List<T> findBy(Consumer<Filterable> consumer, Function<FluentQuery.FetchableFluentQuery<S>, List<T>> queryFunction);

    /**
     * Query based on condition list
     * @param filterable
     * @return
     */
    List<T> findAll(Consumer<Filterable> filterable);

    /**
     * Query based on conditions, sorted
     * @param filterable
     * @param sort
     * @return
     */
    List<T> findAll(Consumer<Filterable> filterable, Sort sort);

    /**
     * Query based on conditions, search by page
     * @param filterable
     * @param pageable
     * @return
     */
    Page<T> findAll(Consumer<Filterable> filterable, Pageable pageable);

    /**
     * Finds a unique value and throws an exception if it does not exist or if there are multiple values
     * @param filterable
     * @return
     */
    T findUniqueOne(Consumer<Filterable> filterable);

    /**
     * Find a unique value. If there are multiple values, null will be returned.
     * @param filterable
     * @return
     */
    Optional<T> findOne(Consumer<Filterable> filterable);

    Long count(Consumer<Filterable> filterable);
}
