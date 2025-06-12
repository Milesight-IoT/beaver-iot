package com.milesight.beaveriot.rule.components.code.language;

import com.milesight.beaveriot.rule.components.code.ExpressionEvaluator;
import org.apache.camel.Exchange;
import org.apache.camel.support.ExpressionSupport;
import org.graalvm.polyglot.*;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * @author leon
 */
public class CustomizedJavaScriptExpression extends ExpressionSupport {

    private final String expressionString;

    public CustomizedJavaScriptExpression(String expressionString) {
        this.expressionString = expressionString;
    }

    public static final String LANG_ID = "js";

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

            Source source = Source.create(LANG_ID, expressionString);
            Value o = cx.eval(source);

            return (T) LanguageHelper.convertResultValue(o, exchange, type);
        }
    }

    @Override
    protected String assertionFailureMessage(Exchange exchange) {
        return this.expressionString;
    }

    @Override
    public String toString() {
        return "JavaScript[" + this.expressionString + "]";
    }
}
