package com.milesight.beaveriot.rule.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.rule.enums.ExpressionLanguage;
import com.milesight.beaveriot.rule.enums.ExpressionOperator;
import com.milesight.beaveriot.rule.model.flow.config.ExpressionConfig;
import lombok.Data;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import static com.milesight.beaveriot.rule.support.SpELExpressionHelper.SPEL_EXPRESSION_PREFIX;
import static com.milesight.beaveriot.rule.support.SpELExpressionHelper.SPEL_EXPRESSION_SUFFIX;

/**
 * @author leon
 */
public class ExpressionGenerator {

    private ExpressionGenerator() {
    }

    public static String generate(String language, List<ExpressionConfig> expressionList, boolean andExpression) {

        Assert.isTrue(!ObjectUtils.isEmpty(expressionList), "expressionList is empty");

        if (ExpressionLanguage.condition.name().equals(language)) {

            List<JsonNode> jsonNodes = expressionList.stream().map(ExpressionConfig::getExpressionValue).toList();

            String split = andExpression ? " && " : " || ";
            List<ConditionExpressionHolder> conditionExpressionHolders = JsonHelper.cast(jsonNodes, new TypeReference<List<ConditionExpressionHolder>>() {
            });

            return wrapperExpression(conditionExpressionHolders.stream().map(ConditionExpressionHolder::getExpression).collect(Collectors.joining(split)));
        } else {
            return getExpressionCodeValue(expressionList.get(0));
        }
    }

    private static String wrapperExpression(String collect) {
        return "#{ " + collect + " }";
    }

    private static String getExpressionCodeValue(ExpressionConfig expressionConfig) {

        Assert.isTrue(expressionConfig != null && expressionConfig.getExpressionValue() != null, "expressionConfig is null");

        return expressionConfig.getExpressionValue().textValue();
    }

    @Data
    public static class ConditionExpressionHolder {

        private String key;
        private String value;
        private ExpressionOperator operator;

        public String getExpression() {
            if (operator == ExpressionOperator.IS_EMPTY || operator == ExpressionOperator.IS_NOT_EMPTY) {
                return MessageFormat.format(operator.getExpression(), retrieveValue(key));
            } else {
                return MessageFormat.format(operator.getExpression(), retrieveValue(key), retrieveValue(value));
            }
        }

        private String retrieveValue(String key) {
            if (ObjectUtils.isEmpty(key)) {
                return key;
            }

            if (key.startsWith(SPEL_EXPRESSION_PREFIX) && key.endsWith(SPEL_EXPRESSION_SUFFIX)) {
                return key.substring(2, key.length() - 1);
            } else {
                return "'" + key + "'";
            }
        }
    }

}
