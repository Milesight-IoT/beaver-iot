package com.milesight.beaveriot.rule.components.code.language;

import com.milesight.beaveriot.rule.components.code.ExpressionEvaluator;
import org.apache.camel.Exchange;
import org.apache.camel.ExpressionIllegalSyntaxException;
import org.apache.camel.language.python.PythonExpression;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * @author leon
 */
public class CustomizedPythonExpression extends PythonExpression {

    private final String expressionString;

    public CustomizedPythonExpression(String expressionString, Class<?> type) {
        super(expressionString, type);
        this.expressionString = expressionString;
    }

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        try (PythonInterpreter compiler = new PythonInterpreter()) {
            compiler.set("exchange", exchange);
            compiler.set("context", exchange.getContext());
            compiler.set("exchangeId", exchange.getExchangeId());
            compiler.set("message", exchange.getMessage());
            compiler.set("headers", exchange.getMessage().getHeaders());
            compiler.set("properties", exchange.getAllProperties());
            compiler.set("body", exchange.getMessage().getBody());

            // Add input variables to the context
            Object inputVariables = exchange.getIn().getHeader(ExpressionEvaluator.HEADER_INPUT_VARIABLES);
            if (!ObjectUtils.isEmpty(inputVariables) && inputVariables instanceof Map) {
                Map<String, Object> inputVariablesMap = (Map<String, Object>) inputVariables;
                inputVariablesMap.forEach(compiler::set);
                exchange.getIn().removeHeader(ExpressionEvaluator.HEADER_INPUT_VARIABLES);
            }

            PyObject out = compiler.eval(expressionString);
            if (out != null) {
                String value = out.toString();
                return exchange.getContext().getTypeConverter().convertTo(type, value);
            }
        } catch (Exception e) {
            throw new ExpressionIllegalSyntaxException(expressionString, e);
        }
        return null;
    }
}
