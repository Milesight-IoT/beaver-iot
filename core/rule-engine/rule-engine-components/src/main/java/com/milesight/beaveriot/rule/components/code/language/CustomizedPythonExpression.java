package com.milesight.beaveriot.rule.components.code.language;

import com.milesight.beaveriot.rule.components.code.ExpressionEvaluator;
import com.milesight.beaveriot.rule.components.code.language.module.LanguageModule;
import com.milesight.beaveriot.rule.components.code.language.module.PythonJsonModule;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.support.ExpressionSupport;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * @author leon
 *
 *  Slow performance: https://www.graalvm.org/jdk22/reference-manual/python/Performance/
 */
public class CustomizedPythonExpression extends ExpressionSupport {
    private static final String MAIN_FUNCTION = "main";
    private final String expressionString;
    public static final String LANG_ID = "python";

    public CustomizedPythonExpression(String expressionString) {
        this.expressionString = expressionString;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        try (Context cx = LanguageHelper.newContext(LANG_ID)) {
            LanguageModule jsonModule = new PythonJsonModule(cx);

            Value b = cx.getBindings(LANG_ID); // Significant performance issue here.

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
            Value expressionOut = cx.eval(source);
            Value function = expressionOut.hasMembers() ? expressionOut.getMember(MAIN_FUNCTION) : null;
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
