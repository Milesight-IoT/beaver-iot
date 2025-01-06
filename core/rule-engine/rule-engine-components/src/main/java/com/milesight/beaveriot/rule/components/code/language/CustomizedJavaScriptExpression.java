package com.milesight.beaveriot.rule.components.code.language;

import com.milesight.beaveriot.rule.components.code.ExpressionEvaluator;
import com.milesight.beaveriot.rule.support.JsonHelper;
import org.apache.camel.Exchange;
import org.apache.camel.language.js.JavaScriptExpression;
import org.apache.camel.language.js.JavaScriptHelper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;

import static org.graalvm.polyglot.Source.newBuilder;

/**
 * @author leon
 */
public class CustomizedJavaScriptExpression extends JavaScriptExpression {

    private final String expressionString;

    public CustomizedJavaScriptExpression(String expressionString, Class<?> type) {
        super(expressionString, type);
        this.expressionString = expressionString;
    }

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        try (Context cx = JavaScriptHelper.newContext()) {
            Value b = cx.getBindings("js");

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

            Source source = newBuilder("js", expressionString, "Unnamed")
                    .mimeType("application/javascript+module").buildLiteral();
            Value o = cx.eval(source);

            return (T) convertValue(o, exchange, type);
        }
    }

    private Object convertValue(Value value, Exchange exchange, Class<?> type) {
        if (value == null) {
            return null;
        }
        if (value.isNumber()) {
            return value.as(Number.class);
        } else if (value.isBoolean()) {
            return value.as(Boolean.class);
        } else {
            Object out = value != null ? value.as(Object.class) : null;
            if (out instanceof List<?>) {
                return JsonHelper.cast(out, List.class);
            } else if (out instanceof Map) {
                return JsonHelper.cast(out, Map.class);
            } else {
                return exchange.getContext().getTypeConverter().convertTo(type, exchange, out);
            }
        }
    }

}
