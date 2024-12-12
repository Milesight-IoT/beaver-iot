package com.milesight.beaveriot.rule.model.flow.route.base;

import com.milesight.beaveriot.rule.model.flow.route.ExpressionNode;

/**
 * @author leon
 */
public interface ExpressionAware {

    ExpressionNode getExpression();

    default String getExpressionProperty() {
        return "expression";
    }
}
