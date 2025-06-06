package com.milesight.beaveriot.rule.model.flow;

import com.milesight.beaveriot.rule.enums.ExpressionLanguage;
import com.milesight.beaveriot.rule.enums.LogicOperator;
import com.milesight.beaveriot.rule.model.flow.config.RuleChoiceConfig;
import com.milesight.beaveriot.rule.support.ExpressionGenerator;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.expression.common.TemplateParserContext;
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

        boolean isAnd = whenConfig.getLogicOperator() == null || whenConfig.getLogicOperator() == LogicOperator.AND;
        String generate = ExpressionGenerator.generate(whenConfig.getExpressionType(), whenConfig.getConditions(), isAnd);
        String expressionType = whenConfig.getExpressionType().equals(ExpressionLanguage.condition.name()) ? ExpressionLanguage.spel.name() : whenConfig.getExpressionType();
        return create(expressionType, generate);
    }

    public boolean validate() {
        return !ObjectUtils.isEmpty(language) && !ObjectUtils.isEmpty(expression);
    }

    @Override
    public String toString() {
        return "ExpressionNode{" +
                    "language=" + language +
                    ", expression=" + SpELExpressionHelper.wrapTemplateIfNeeded(expression) +
                "}";
    }

}