package com.milesight.beaveriot.rule.support;

import com.milesight.beaveriot.rule.model.RuleLanguage;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.builder.ExpressionBuilder;
import org.springframework.util.ObjectUtils;

/**
 * @author leon
 */
public class ExpressionHelper {

    private ExpressionHelper() {
    }

    public static Object resolveExpression(Exchange exchange, String expressionValue) {
        if (ObjectUtils.isEmpty(expressionValue)) {
            return null;
        }
        //todo  spel parser
        if (containSpELExpression(expressionValue)) {
            Expression expression = ExpressionBuilder.languageExpression(RuleLanguage.LANGUAGE_SPEL, expressionValue);
            expression.init(exchange.getContext());
            return expression.evaluate(exchange, Object.class);
        } else {
            return expressionValue;
        }
    }

    private static boolean containSpELExpression(String stringValue) {
        return !ObjectUtils.isEmpty(stringValue) && stringValue.contains("${");
    }

}
