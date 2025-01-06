package com.milesight.beaveriot.rule.components.code;

import com.milesight.beaveriot.rule.enums.ExpressionLanguage;
import com.milesight.beaveriot.rule.model.flow.ExpressionNode;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;

import java.util.Map;

import static org.apache.camel.support.builder.ExpressionBuilder.languageExpression;

/**
 * @author leon
 */
public class ExpressionEvaluator {

    public static final String HEADER_INPUT_VARIABLES = "_inputVariables";

    private ExpressionEvaluator() {
    }

    public static <T> T evaluate(ExpressionNode expression, Exchange exchange, Map<String, Object> inputVariables, Class<T> type) {

        ExpressionNode wrapperExpressionNode = prepareEvaluationContext(expression, exchange, inputVariables);

        Expression languageExpression = languageExpression(wrapperExpressionNode.getLanguage(), wrapperExpressionNode.getExpression());

        languageExpression.init(exchange.getContext());

        return languageExpression.evaluate(exchange, type);
    }

    private static ExpressionNode prepareEvaluationContext(ExpressionNode expression, Exchange exchange, Map<String, Object> inputVariables) {
        exchange.getIn().setHeader(HEADER_INPUT_VARIABLES, inputVariables);
        if (expression.getLanguage().equals(ExpressionLanguage.js.name())) {
            String wrapperExpression = "(()=>{ " +
                    expression.getExpression() +
                    "      })()";
            return ExpressionNode.create(expression.getLanguage(), wrapperExpression);
        }
        return expression;
    }

}
