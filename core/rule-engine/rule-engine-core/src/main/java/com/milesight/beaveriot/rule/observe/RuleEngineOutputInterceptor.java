package com.milesight.beaveriot.rule.observe;

import com.milesight.beaveriot.rule.flow.ComponentDefinitionCache;
import com.milesight.beaveriot.rule.model.definition.ComponentOutputDefinition;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.support.ComponentParameterConverter;
import com.milesight.beaveriot.rule.support.RuleFlowIdGenerator;
import org.apache.camel.*;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.spi.InterceptStrategy;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author leon
 */
public class RuleEngineOutputInterceptor implements InterceptStrategy {

    @Override
    public Processor wrapProcessorInInterceptors(CamelContext context, NamedNode definition, Processor target, Processor nextTarget) throws Exception {
        return exchange -> {

            target.process(exchange);

            cacheOutputArguments(definition, exchange);
        };
    }

    protected void cacheOutputArguments(NamedNode definition, Exchange exchange) {
        String configNodeId = RuleFlowIdGenerator.removeNamespacedId(exchange.getFromRouteId(), definition.getId());
        RuleNodeConfig ruleConfig = RuleConfigOutputCache.get(exchange.getFromRouteId(), configNodeId);
        if (ruleConfig == null) {
            return;
        }

        Collection<ComponentOutputDefinition> outputDefinitions = ComponentDefinitionCache.loadOutputArguments(ruleConfig.getComponentId());
        if (!CollectionUtils.isEmpty(outputDefinitions)) {
            Map<String, Object> nodeContext = exchange.getProperty(configNodeId, new LinkedHashMap<>(), Map.class);
            outputDefinitions.forEach(outputDefinition -> {
                if (outputDefinition.getInputDefinition() != null) {
                    nodeContext.put(outputDefinition.getName(), parserValue(exchange, ruleConfig, outputDefinition));
                } else {
                    nodeContext.put(outputDefinition.getName(), exchange.getIn().getBody());
                }
            });
            exchange.setProperty(configNodeId, nodeContext);
        }
    }

    private Object parserValue(Exchange exchange, RuleNodeConfig ruleConfig, ComponentOutputDefinition outputDefinition) {
        Object parameterValue = ComponentParameterConverter.getParameterValue(ruleConfig.getParameters(), outputDefinition.getInputDefinition().getFullName());
        if (parameterValue == null) {
            return null;
        }
        //todo  spel parser
        if (parameterValue instanceof String stringValue && containSpelExpression(stringValue)) {
            Expression expression = ExpressionBuilder.languageExpression("spel", stringValue);
            expression.init(exchange.getContext());
            return expression.evaluate(exchange, Object.class);
        } else {
            return parameterValue;
        }
    }

    private boolean containSpelExpression(String stringValue) {
        return !ObjectUtils.isEmpty(stringValue) && stringValue.contains("${");
    }
}
