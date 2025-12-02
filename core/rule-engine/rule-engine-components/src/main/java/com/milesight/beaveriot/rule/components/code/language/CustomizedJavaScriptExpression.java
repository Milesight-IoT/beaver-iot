package com.milesight.beaveriot.rule.components.code.language;

import com.milesight.beaveriot.rule.components.code.ExpressionEvaluator;
import com.milesight.beaveriot.rule.components.code.language.module.JavaScriptJsonModule;
import com.milesight.beaveriot.rule.components.code.language.module.LanguageModule;
import org.apache.camel.Exchange;
import org.apache.camel.support.ExpressionSupport;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * @author leon
 */
public class CustomizedJavaScriptExpression extends ExpressionSupport {
    private final String expressionString;
    public static final String LANG_ID = "js";

    public CustomizedJavaScriptExpression(String expressionString) {
        this.expressionString = expressionString;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        try (Context cx = LanguageHelper.newContext(LANG_ID)) {
            LanguageModule jsonModule = new JavaScriptJsonModule(cx);

            Value b = cx.getBindings(LANG_ID);

            b.putMember("exchange", exchange);
            b.putMember("context", exchange.getContext());
            b.putMember("exchangeId", exchange.getExchangeId());
            b.putMember("message", exchange.getMessage());
            b.putMember("headers", exchange.getMessage().getHeaders());
            b.putMember("properties", exchange.getAllProperties());
            b.putMember("body", jsonModule.input(exchange.getMessage().getBody()));

            // Add input variables to the context
            Object inputVariables = exchange.getIn().getHeader(ExpressionEvaluator.HEADER_INPUT_VARIABLES);
            if (!ObjectUtils.isEmpty(inputVariables) && inputVariables instanceof Map) {
                Map<String, Object> inputVariablesMap = (Map<String, Object>) inputVariables;
                inputVariablesMap.forEach((k, v) -> {
                    Object value = jsonModule.input(v);
                    b.putMember(k, value);
                });
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
