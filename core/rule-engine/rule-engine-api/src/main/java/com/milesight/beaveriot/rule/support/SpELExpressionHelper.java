package com.milesight.beaveriot.rule.support;

import com.milesight.beaveriot.rule.enums.ExpressionLanguage;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.CompositeStringExpression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String,Object> result = new HashMap<>();
        expressionMap.entrySet().stream().forEach(entry -> result.put((String) resolveStringExpression(exchange, entry.getKey()), resolveStringExpression(exchange, entry.getValue())));
        return result;
    }

    public static List<String> resolveExpression(Exchange exchange, List<String> expressionList) {
        if (ObjectUtils.isEmpty(expressionList)) {
            return List.of();
        }
        return expressionList.stream().map(expression -> (String) resolveStringExpression(exchange, expression)).toList();
    }

    public static Object resolveStringExpression(Exchange exchange, Object expressionValue) {
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

    public static SpelExpression[] extractIfSpELExpression(Object value) {
        if (ObjectUtils.isEmpty(value)) {
            return new SpelExpression[0];
        }
        String stringValue = value instanceof String ? (String) value : value.toString();
        if (containSpELExpression(stringValue)) {
            return extractSpELExpression(stringValue, new TemplateParserContext());
        }
        return new SpelExpression[0];
    }

    private static SpelExpression[] extractSpELExpression(String spelExpressionStr, @Nullable ParserContext parserContext) {
        SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
        org.springframework.expression.Expression expression = parserContext == null ? spelExpressionParser.parseExpression(spelExpressionStr) : spelExpressionParser.parseExpression(spelExpressionStr, parserContext);
        if (expression instanceof SpelExpression spelExpression) {
            return new SpelExpression[]{spelExpression};
        } else if (expression instanceof CompositeStringExpression compositeStringExpression) {
            return Arrays.stream(compositeStringExpression.getExpressions())
                    .filter(SpelExpression.class::isInstance)
                    .toArray(SpelExpression[]::new);
        } else {
            return new SpelExpression[0];
        }
    }

    public static SpelExpression[] extractSpELExpression(String spelExpressionStr) {
        return extractSpELExpression(spelExpressionStr, null);
    }

    public static String wrapTemplateIfNeeded(String expression) {
        return containSpELExpression(expression) ? expression : SPEL_EXPRESSION_PREFIX + expression + SPEL_EXPRESSION_SUFFIX;
    }
}
