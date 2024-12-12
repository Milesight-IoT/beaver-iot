package com.milesight.beaveriot.rule.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.rule.enums.ExpressionOperator;
import com.milesight.beaveriot.rule.model.RuleLanguage;
import com.milesight.beaveriot.rule.model.flow.config.ExpressionConfig;
import lombok.Data;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author leon
 */
public class ExpressionGenerator {

    public static String generate(String language, List<ExpressionConfig> expressionList, boolean andExpression) {

        Assert.isTrue(!ObjectUtils.isEmpty(expressionList), "expressionList is empty");

        //todo:
        if (RuleLanguage.LANGUAGE_CONDITION.equals(language)) {

            List<JsonNode> jsonNodes = expressionList.stream().map(ExpressionConfig::getExpressionValue).toList();

            String split = andExpression ? " and " : " or ";
            List<ConditionExpressionHolder> conditionExpressionHolders = JSONHelper.cast(jsonNodes, new TypeReference<List<ConditionExpressionHolder>>() {
            });

            return conditionExpressionHolders.stream().map(ConditionExpressionHolder::getExpression).collect(Collectors.joining(split));
        } else {
            return getExpressionCodeValue(expressionList.get(0));
        }
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
            return key + " " + operator.name() + " " + value;
        }
    }

}
