package com.milesight.beaveriot.data.filterable.condition;

import com.milesight.beaveriot.data.filterable.enums.BooleanOperator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leon
 */
public abstract class CompositeCondition implements Condition {

    protected BooleanOperator operator = BooleanOperator.AND;
    /** Query conditions */
    protected List<Condition> searchConditions = new ArrayList<>();

    protected CompositeCondition(BooleanOperator operator, List<Condition> searchConditions) {
        this.operator = operator;
        this.searchConditions = searchConditions;
    }

    public BooleanOperator getBooleanOperator() {
        return operator;
    }

    public List<Condition> getConditions() {
        return searchConditions;
    }
}
