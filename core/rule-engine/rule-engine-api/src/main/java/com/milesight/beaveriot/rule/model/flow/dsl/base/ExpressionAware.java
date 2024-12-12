package com.milesight.beaveriot.rule.model.flow.dsl.base;

import com.milesight.beaveriot.rule.model.flow.dsl.ExpressionNode;

/**
 * @author leon
 */
public interface ExpressionAware {

    ExpressionNode getExpression();

    default String getExpressionProperty() {
        return "expression";
    }
}
