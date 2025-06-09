package com.milesight.beaveriot.rule.components.code.language;

import com.milesight.beaveriot.rule.components.code.ExpressionEvaluator;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.support.ExpressionSupport;
import org.graalvm.polyglot.*;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * @author leon
 */
public class CustomizedPythonExpression extends ExpressionSupport {

    private static final String MAIN_FUNCTION = "main";
    private final String expressionString;

    public static final String LANG_ID = "python";

    public CustomizedPythonExpression(String expressionString) {
        this.expressionString = expressionString;
    }

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {

        try (Context cx = LanguageHelper.newContext(LANG_ID)) {
            Value b = cx.getBindings(LANG_ID);

            b.putMember("exchange", exchange);
            b.putMember("context", exchange.getContext());
            b.putMember("exchangeId", exchange.getExchangeId());
            b.putMember("message", exchange.getMessage());
            b.putMember("headers", exchange.getMessage().getHeaders());
            b.putMember("properties", exchange.getAllProperties());
            b.putMember("body", exchange.getMessage().getBody());

            // Add input variables to the context
            Object inputVariables = exchange.getIn().getHeader(ExpressionEvaluator.HEADER_INPUT_VARIABLES);
            if (!ObjectUtils.isEmpty(inputVariables) && inputVariables instanceof Map) {
                Map<String, Object> inputVariablesMap = (Map<String, Object>) inputVariables;
                inputVariablesMap.forEach(b::putMember);
                exchange.getIn().removeHeader(ExpressionEvaluator.HEADER_INPUT_VARIABLES);
            }

            Value expressionOut = cx.eval(LANG_ID, expressionString);
            Value function = b.getMember(MAIN_FUNCTION);
            if (function == null) {
                return (T) LanguageHelper.convertResultValue(expressionOut, exchange, type);
            }

            Value out = function.execute();
            if (out != null) {
                return (T) LanguageHelper.convertResultValue(out, exchange, type);
            }
        } catch (Exception e) {
            throw new RuntimeCamelException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    protected String assertionFailureMessage(Exchange exchange) {
        return this.expressionString;
    }

    @Override
    public String toString() {
        return "Python[" + this.expressionString + "]";
    }
}
