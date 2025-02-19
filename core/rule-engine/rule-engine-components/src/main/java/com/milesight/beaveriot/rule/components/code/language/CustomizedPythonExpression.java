package com.milesight.beaveriot.rule.components.code.language;

import com.milesight.beaveriot.rule.components.code.ExpressionEvaluator;
import org.apache.camel.Exchange;
import org.apache.camel.ExpressionIllegalSyntaxException;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.language.python.PythonExpression;
import org.python.core.PyBoolean;
import org.python.core.PyObject;
import org.python.core.PySingleton;
import org.python.core.PyUnicode;
import org.python.util.PythonInterpreter;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;

/**
 * @author leon
 */
public class CustomizedPythonExpression extends PythonExpression {

    private final static String MAIN_FUNCTION = "main";
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

            compiler.exec(expressionString);
            PyObject function = compiler.get(MAIN_FUNCTION);
            Assert.notNull(function, "Main function not found in the script");
            PyObject out = function.__call__();
            if (out != null) {
                return (T) convertValue(out);
            }
        } catch (Exception e) {
            throw new RuntimeCamelException(e.getMessage(), e);
        }
        return null;
    }

    private Object convertValue(PyObject out) {
        Object result = null;
        if (out instanceof PyBoolean) {
            result = out.__tojava__(Boolean.class);
        } else if (out instanceof PyUnicode) {
            result = out.__tojava__(String.class);
        } else if (out.isNumberType()) {
            result = out.__tojava__(Number.class);
        } else if (out.isMappingType()) {
            result = out.__tojava__(Map.class);
        } else if (out.isSequenceType()) {
            result = out.__tojava__(List.class);
        } else {
            result = out.toString();
        }

        //todo:
        if (result instanceof PySingleton pySingleton && "Error".equals(pySingleton.toString())) {
            throw new ExpressionIllegalSyntaxException(expressionString, new Exception(pySingleton.toString()));
        }

        return result;
    }

}
