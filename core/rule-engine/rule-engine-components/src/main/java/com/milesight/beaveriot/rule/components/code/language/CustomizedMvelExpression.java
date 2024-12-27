package com.milesight.beaveriot.rule.components.code.language;

import com.milesight.beaveriot.rule.components.code.ExpressionEvaluator;
import org.apache.camel.Exchange;
import org.apache.camel.ExpressionEvaluationException;
import org.apache.camel.ExpressionIllegalSyntaxException;
import org.apache.camel.language.mvel.MvelExpression;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author leon
 */
public class CustomizedMvelExpression extends MvelExpression {

    private final Serializable compiled;

    public CustomizedMvelExpression(String expressionString, Class<?> type) {
        super(expressionString, type);

        try {
            this.compiled = org.mvel2.MVEL.compileExpression(expressionString);
        } catch (Exception e) {
            throw new ExpressionIllegalSyntaxException(expressionString, e);
        }
    }

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        try {

            Object inputVariables = exchange.getIn().getHeader(ExpressionEvaluator.HEADER_INPUT_VARIABLES);
            Map<String, Object> variables = new HashMap<>();
            variables.put("exchange", exchange);
            variables.put("message", exchange.getMessage());
            variables.put("exchangeId", exchange.getExchangeId());
            variables.put("headers", exchange.getIn().getHeaders());
            variables.put("properties", exchange.getAllProperties());
            variables.put("body", exchange.getMessage().getBody());

            if (!ObjectUtils.isEmpty(inputVariables) && inputVariables instanceof Map) {
                Map<String, Object> inputVariablesMap = (Map<String, Object>) inputVariables;
                inputVariablesMap.entrySet().forEach(entry -> variables.put(entry.getKey(), entry.getValue()));
            }
            Object value = org.mvel2.MVEL.executeExpression(compiled, variables);
            return exchange.getContext().getTypeConverter().convertTo(type, value);
        } catch (Exception e) {
            throw new ExpressionEvaluationException(this, exchange, e);
        }
    }
}
