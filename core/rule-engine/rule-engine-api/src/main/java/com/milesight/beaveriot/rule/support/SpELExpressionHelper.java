package com.milesight.beaveriot.rule.support;

import com.milesight.beaveriot.rule.enums.ExpressionLanguage;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.camel.support.builder.ExpressionBuilder.languageExpression;

/**
 * @author leon
 */
public class SpELExpressionHelper {

    public static final String SPEL_EXPRESSION_PREFIX = "#{";
    public static final String SPEL_EXPRESSION_SUFFIX = "}";

    private SpELExpressionHelper() {
    }

    public static Map<String, Object> resolveExpression(Exchange exchange, Map<String, Object> expressionMap) {
        if (ObjectUtils.isEmpty(expressionMap)) {
            return Map.of();
        }
        return expressionMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> resolveExpression(exchange, entry.getValue())));
    }

    public static List<String> resolveExpression(Exchange exchange, List<String> expressionList) {
        if (ObjectUtils.isEmpty(expressionList)) {
            return List.of();
        }
        return expressionList.stream().map(expression -> (String) resolveExpression(exchange, expression)).toList();
    }

    public static Object resolveExpression(Exchange exchange, Object expressionValue) {
        if (ObjectUtils.isEmpty(expressionValue)) {
            return expressionValue;
        }
        if (containSpELExpression(expressionValue)) {
            Expression expression = languageExpression(ExpressionLanguage.spel.name(), (String) expressionValue);
            expression.init(exchange.getContext());
            return expression.evaluate(exchange, Object.class);
        } else {
            return expressionValue;
        }
    }

    private static boolean containSpELExpression(Object stringValue) {
        return stringValue instanceof String value && value.contains(SPEL_EXPRESSION_PREFIX);
    }

}
