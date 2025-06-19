package com.milesight.beaveriot.rule.components.code;

import com.milesight.beaveriot.rule.RuleEngineRouteConfigurer;
import com.milesight.beaveriot.rule.components.code.language.CustomizedJavaScriptLanguage;
import com.milesight.beaveriot.rule.components.code.language.CustomizedMvelLanguage;
import com.milesight.beaveriot.rule.components.code.language.CustomizedPythonLanguage;
import com.milesight.beaveriot.rule.components.code.language.LanguageWarmUp;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.language.groovy.GroovyShellFactory;
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
        bindRegistry(context, "groovyShellFactory", new CustomizedGroovyShellFactory());
        bindRegistry(context, "mvel-language", new CustomizedMvelLanguage());
        bindRegistry(context, "js-language", new CustomizedJavaScriptLanguage());
        bindRegistry(context, "python-language", new CustomizedPythonLanguage());
    }

    public void bindRegistry(CamelContext context, String name, Object lang) {
        context.getRegistry().bind(name, lang);
        if (LanguageWarmUp.class.isAssignableFrom(lang.getClass())) {
            new Thread(() -> {
                log.info("[Language-Warming] {} start", name);
                ((LanguageWarmUp) lang).warmUp();
                log.info("[Language-Warming] {} done", name);
            }).start();
        }
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
