package com.milesight.beaveriot.rule.model.flow.route;

import com.milesight.beaveriot.rule.model.flow.config.RuleChoiceConfig;
import com.milesight.beaveriot.rule.support.ExpressionGenerator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.ObjectUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpressionNode {

    private String language;
    private String expression;

    public static ExpressionNode create(String language, String expression) {
        return new ExpressionNode(language, expression);
    }

    public static ExpressionNode create(RuleChoiceConfig.RuleChoiceWhenConfig whenConfig) {

        if (!ObjectUtils.isEmpty(whenConfig.getAnd())) {
            String generate = ExpressionGenerator.generate(whenConfig.getExpressionType(), whenConfig.getAnd(), true);
            return create(whenConfig.getExpressionType(), generate);
        } else if (!ObjectUtils.isEmpty(whenConfig.getOr())) {
            String generate = ExpressionGenerator.generate(whenConfig.getExpressionType(), whenConfig.getOr(), false);
            return create(whenConfig.getExpressionType(), generate);
        } else {
            throw new IllegalArgumentException("And or Or must be set");
        }
    }

    public boolean validate() {
        return !ObjectUtils.isEmpty(language) && !ObjectUtils.isEmpty(expression);
    }
}