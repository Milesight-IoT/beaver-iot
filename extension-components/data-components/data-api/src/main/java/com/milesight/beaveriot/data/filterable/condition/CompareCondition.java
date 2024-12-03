package com.milesight.beaveriot.data.filterable.condition;

import com.milesight.beaveriot.data.filterable.enums.SearchOperator;

/**
 * @author leon
 */
public class CompareCondition implements Condition{

    private String name;
    protected SearchOperator searchOperator;
    private Object value;

    public CompareCondition(String name, Object value, SearchOperator searchOperator) {
        this.name = name;
        this.value = value;
        this.searchOperator = searchOperator;
    }

    public String getName() {
        return name;
    }

    public SearchOperator getSearchOperator() {
        return searchOperator;
    }

    public Object getValue() {
        return value;
    }
}
