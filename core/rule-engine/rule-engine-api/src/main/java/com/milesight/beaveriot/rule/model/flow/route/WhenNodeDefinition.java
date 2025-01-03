package com.milesight.beaveriot.rule.model.flow.route;

import com.milesight.beaveriot.rule.model.flow.ExpressionNode;
import lombok.Data;
import org.springframework.util.Assert;

/**
 * @author leon
 */
@Data
public class WhenNodeDefinition extends AbstractNodeDefinition {

    private ExpressionNode expression;

    public WhenNodeDefinition(ExpressionNode expression) {
        Assert.notNull(expression, "Expression must not be null");
        this.expression = expression;
    }

}
