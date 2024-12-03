package com.milesight.beaveriot.data.filterable;

import com.milesight.beaveriot.data.filterable.condition.CompareCondition;
import com.milesight.beaveriot.data.filterable.condition.CompositeCondition;
import com.milesight.beaveriot.data.filterable.condition.Condition;
import com.milesight.beaveriot.data.filterable.enums.BooleanOperator;
import com.milesight.beaveriot.data.filterable.enums.SearchOperator;
import org.springframework.data.util.Pair;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author leon
 */
public class SearchFilter extends CompositeCondition implements Filterable{

    public SearchFilter(BooleanOperator operator, List<Condition> searchConditions) {
        super(operator, searchConditions);
    }

    @Override
    public Filterable likeIgnoreCase(String name, String value) {
        return addCompareCondition(!ObjectUtils.isEmpty(value), SearchOperator.CASE_IGNORE_LIKE, name, value);
    }

    @Override
    public Filterable likeIgnoreCase(boolean condition, String name, String value) {
        return addCompareCondition(condition, SearchOperator.CASE_IGNORE_LIKE, name, value);
    }

    @Override
    public Filterable like(String name, String value) {
        return addCompareCondition(!ObjectUtils.isEmpty(value), SearchOperator.LIKE, name, value);
    }

    private Filterable addCompareCondition(boolean condition, SearchOperator searchOperator, String name, Object value){
        if(condition){
            this.searchConditions.add(new CompareCondition(name, value,  searchOperator));
        }
        return this;
    }

    @Override
    public Filterable like(boolean condition, String name, String value) {
        return addCompareCondition(condition, SearchOperator.LIKE, name, value);
    }

    @Override
    public Filterable notLike(String name, String value) {
        return addCompareCondition(!ObjectUtils.isEmpty(value), SearchOperator.NOT_LIKE, name, value);
    }

    @Override
    public Filterable notLike(boolean condition, String name, String value) {
        return addCompareCondition(condition, SearchOperator.NOT_LIKE, name, value);
    }

    @Override
    public Filterable likeLeft(String name, String value) {
        return addCompareCondition(!ObjectUtils.isEmpty(value), SearchOperator.LIKE_LEFT, name, value);
    }

    @Override
    public Filterable likeLeft(boolean condition, String name, String value) {
        return addCompareCondition(condition, SearchOperator.LIKE_LEFT, name, value);
    }

    @Override
    public Filterable likeRight(String name, String value) {
        return addCompareCondition(!ObjectUtils.isEmpty(value), SearchOperator.LIKE_RIGHT, name, value);
    }

    @Override
    public Filterable likeRight(boolean condition, String name, String value) {
        return addCompareCondition(condition, SearchOperator.LIKE_LEFT, name, value);
    }

    @Override
    public Filterable allEq(Map<String, Object> params) {
        if(!ObjectUtils.isEmpty(params)){
            params.entrySet().stream().forEach(entry->eq(entry.getKey(), entry.getValue()));
        }
        return this;
    }

    @Override
    public Filterable allEq(boolean condition, Map<String, Object> params) {
        if(!ObjectUtils.isEmpty(params)){
            params.entrySet().stream().forEach(entry->eq(condition,entry.getKey(), entry.getValue()));
        }
        return this;
    }
    @Override
    public Filterable eq(String name, Object value) {
        return addCompareCondition(!ObjectUtils.isEmpty(value), SearchOperator.EQ, name, value);
    }

    @Override
    public Filterable eq(boolean condition, String name, Object value) {
        return addCompareCondition(condition, SearchOperator.EQ, name, value);
    }

    @Override
    public Filterable ne(String name, Object value) {
        return addCompareCondition(!ObjectUtils.isEmpty(value), SearchOperator.NE, name, value);
    }

    @Override
    public Filterable ne(boolean condition, String name, Object value) {
        return addCompareCondition(condition, SearchOperator.EQ, name, value);
    }

    @Override
    public Filterable gt(String name, Object value) {
        return addCompareCondition(!ObjectUtils.isEmpty(value), SearchOperator.GT, name, value);
    }

    @Override
    public Filterable gt(boolean condition, String name, Object value) {
        return addCompareCondition(condition, SearchOperator.EQ, name, value);
    }

    @Override
    public Filterable ge(String name, Object value) {
        return addCompareCondition(!ObjectUtils.isEmpty(value), SearchOperator.GE, name, value);
    }

    @Override
    public Filterable ge(boolean condition, String name, Object value) {
        return addCompareCondition(condition, SearchOperator.GE, name, value);
    }

    @Override
    public Filterable lt(String name, Object value) {
        return addCompareCondition(!ObjectUtils.isEmpty(value), SearchOperator.LT, name, value);
    }

    @Override
    public Filterable lt(boolean condition, String name, Object value) {
        return addCompareCondition(condition, SearchOperator.LT, name, value);
    }

    @Override
    public Filterable le(String name, Object value) {
        return addCompareCondition(!ObjectUtils.isEmpty(value), SearchOperator.LE, name, value);
    }

    @Override
    public Filterable le(boolean condition, String name, Object value) {
        return addCompareCondition(condition, SearchOperator.LE, name, value);
    }

    @Override
    public Filterable between(String name, Object min, Object max) {
        Assert.notNull(min, "min value must not be null");
        Assert.notNull(max, "max value must not be null");
        return addCompareCondition((!ObjectUtils.isEmpty(min) && !ObjectUtils.isEmpty(max)) , SearchOperator.BETWEEN, name, Pair.of(min, max));
    }

    @Override
    public Filterable isNull(String name) {
        return addCompareCondition(true , SearchOperator.IS_NULL, name, null);
    }

    @Override
    public Filterable isNotNull(String name) {
        return addCompareCondition(true , SearchOperator.IS_NOT_NULL, name, null);
    }

    @Override
    public Filterable isNull(boolean condition, String name) {
        return addCompareCondition(condition , SearchOperator.IS_NULL, name, null);
    }

    @Override
    public Filterable isNotNull(boolean condition, String name) {
        return addCompareCondition(condition , SearchOperator.IS_NOT_NULL, name, null);
    }

    @Override
    public Filterable in(String name, Object[] value) {
        Assert.isTrue(!ObjectUtils.isEmpty(value), "value must not be null");
        return addCompareCondition(!ObjectUtils.isEmpty(value) , SearchOperator.IN, name, value);
    }

    @Override
    public Filterable in(boolean condition, String name, Object[] value) {
        return addCompareCondition(condition , SearchOperator.IN, name, value);
    }

    @Override
    public Filterable notIn(String name, Object[] value) {
        Assert.isTrue(!ObjectUtils.isEmpty(value), "value must not be null");
        return addCompareCondition(!ObjectUtils.isEmpty(value) , SearchOperator.NOT_IN, name, value);
    }

    @Override
    public Filterable notIn(boolean condition, String name, Object[] value) {
        return addCompareCondition(condition , SearchOperator.NOT_IN, name, value);
    }

    @Override
    public Filterable addCondition(String name, Object value, SearchOperator searchOperator) {
        return addCompareCondition(!ObjectUtils.isEmpty(value) , searchOperator, name, value);
    }

    @Override
    public Filterable or(Consumer<Filterable> consumer) {
        SearchFilter nextQuerySpecs = new SearchFilter(BooleanOperator.OR, new ArrayList<>());
        consumer.accept(nextQuerySpecs);
        searchConditions.add(nextQuerySpecs);
        return nextQuerySpecs;
    }

    @Override
    public Filterable and(Consumer<Filterable> consumer) {
        SearchFilter nextQuerySpecs = new SearchFilter(BooleanOperator.AND, new ArrayList<>());
        consumer.accept(nextQuerySpecs);
        searchConditions.add(nextQuerySpecs);
        return nextQuerySpecs;
    }

}
