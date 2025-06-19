package com.milesight.beaveriot.rule.components.code;

import com.milesight.beaveriot.rule.RuleEngineRouteConfigurer;
import com.milesight.beaveriot.rule.components.code.language.CustomizedJavaScriptLanguage;
import com.milesight.beaveriot.rule.components.code.language.CustomizedMvelLanguage;
import com.milesight.beaveriot.rule.components.code.language.CustomizedPythonLanguage;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.language.groovy.GroovyShellFactory;
import org.apache.camel.spi.ScriptingLanguage;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author leon
 */
@Component
@Slf4j
public class LanguageRouteConfigurer implements RuleEngineRouteConfigurer {
    @Override
    public void customizeRoute(CamelContext context) throws Exception {
        context.getRegistry().bind("groovyShellFactory", new CustomizedGroovyShellFactory());
        context.getRegistry().bind("mvel-language", new CustomizedMvelLanguage());

        CustomizedJavaScriptLanguage javaScriptLanguage = new CustomizedJavaScriptLanguage();
        context.getRegistry().bind("js-language", javaScriptLanguage);
        log.info("[Language-Warm-Up] js");
        warmUp(javaScriptLanguage);

        CustomizedPythonLanguage pythonLanguage = new CustomizedPythonLanguage();
        context.getRegistry().bind("python-language", pythonLanguage);
        log.info("[Language-Warm-Up] python");
        warmUp(pythonLanguage);
    }

    public void warmUp(ScriptingLanguage lang) {
       lang.evaluate("", Map.of(), Object.class);
    }

    public class CustomizedGroovyShellFactory implements GroovyShellFactory {
        @Override
        public GroovyShell createGroovyShell(Exchange exchange) {
            return new GroovyShell(exchange.getContext().getApplicationContextClassLoader());
        }

        @Override
        public Map<String, Object> getVariables(Exchange exchange) {
            Map<String, Object> variables = new HashMap<>();
            Object inputVariables = exchange.getIn().getHeader(ExpressionEvaluator.HEADER_INPUT_VARIABLES);
            if (!ObjectUtils.isEmpty(inputVariables) && inputVariables instanceof Map) {
                Map<String, Object> inputVariablesMap = (Map<String, Object>) inputVariables;
                variables.putAll(inputVariablesMap);
            }
            variables.put("properties", exchange.getProperties());
            return variables;
        }
    }
}
