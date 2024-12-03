package com.milesight.beaveriot.data.filterable;


import com.milesight.beaveriot.data.filterable.enums.SearchOperator;

import java.util.Map;
import java.util.function.Consumer;

/**
 * @author leon
 */
public interface Filterable {

    Filterable likeIgnoreCase(String name, String value);

    Filterable likeIgnoreCase(boolean condition, String name, String value);

    Filterable like(String name, String value);

    Filterable like(boolean condition, String name, String value);

    Filterable notLike(String name, String value);

    Filterable notLike(boolean condition, String name, String value);

    Filterable likeLeft(String name, String value);

    Filterable likeLeft(boolean condition, String name, String value);

    Filterable likeRight(String name, String value);

    Filterable likeRight(boolean condition, String name, String value);

    Filterable allEq(Map<String,Object> params);

    Filterable allEq(boolean condition, Map<String,Object> params);

    Filterable eq(String name, Object value);

    Filterable eq(boolean condition, String name, Object value);

    Filterable ne(String name, Object value);

    Filterable ne(boolean condition, String name, Object value);

    Filterable gt(String name, Object value);

    Filterable gt(boolean condition, String name, Object value);

    Filterable ge(String name, Object value);

    Filterable ge(boolean condition, String name, Object value);

    Filterable lt(String name, Object value);

    Filterable lt(boolean condition, String name, Object value);

    Filterable le(String name, Object value);

    Filterable le(boolean condition, String name, Object value);

    Filterable between(String name, Object min, Object max);

    Filterable isNull(String name);

    Filterable isNotNull(String name);

    Filterable isNull(boolean condition, String name);

    Filterable isNotNull(boolean condition, String name);

    Filterable in(String name, Object[] value);

    Filterable in(boolean condition, String name, Object[] value);


    Filterable notIn(String name, Object[] value);

    Filterable notIn(boolean condition, String name, Object[] value);

    /**
     * You can specify query column names, value values and operators (to facilitate dynamic construction of the framework)
     * When Function is queried, value is empty, when Between is queried, the input parameter is Pair, and when in is queried, the input parameter is array.
     * @param name
     * @param value
     * @param searchOperator
     * @return
     */
    Filterable addCondition(String name, Object value, SearchOperator searchOperator);

    Filterable or(Consumer<Filterable> consumer);

    Filterable and(Consumer<Filterable> consumer);

}
