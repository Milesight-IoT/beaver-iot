package com.milesight.beaveriot.rule.trace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.rule.RuleNodeDefinitionInterceptor;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import com.milesight.beaveriot.rule.flow.ComponentDefinitionCache;
import com.milesight.beaveriot.rule.model.VariableNamed;
import com.milesight.beaveriot.rule.model.definition.ComponentDefinition;
import com.milesight.beaveriot.rule.model.definition.ComponentOptionDefinition;
import com.milesight.beaveriot.rule.model.definition.ComponentOptionExtensionDefinition;
import com.milesight.beaveriot.rule.model.definition.ComponentOutputDefinition;
import com.milesight.beaveriot.rule.model.flow.route.ChoiceNodeDefinition;
import com.milesight.beaveriot.rule.model.flow.route.FromNodeDefinition;
import com.milesight.beaveriot.rule.model.flow.route.ToNodeDefinition;
import com.milesight.beaveriot.rule.support.ExpressionGenerator;
import com.milesight.beaveriot.rule.support.JsonHelper;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.milesight.beaveriot.rule.constants.RuleNodeNames.*;

/**
 * @author leon
 */
public class RuleNodeLogVariablesInterceptor implements RuleNodeDefinitionInterceptor {

    @Override
    public FromNodeDefinition interceptFromNodeDefinition(String flowId, FromNodeDefinition fromNode) {
        doInterceptNodeDefinition(flowId, fromNode.getId(), fromNode.getParameters(), obtainComponentId(fromNode.getUri(), fromNode.getParameters()));
        return fromNode;
    }

    @Override
    public ChoiceNodeDefinition interceptChoiceNodeDefinition(String flowId, ChoiceNodeDefinition choiceNodeDefinition) {
        doInterceptNodeDefinition(flowId, choiceNodeDefinition.getId(), choiceNodeDefinition.getWhenNodeDefinitions(), "choice");
        return choiceNodeDefinition;
    }

    @Override
    public ToNodeDefinition interceptToNodeDefinition(String flowId, ToNodeDefinition toNodeDefinition) {
        doInterceptNodeDefinition(flowId, toNodeDefinition.getId(), toNodeDefinition.getParameters(), obtainComponentId(toNodeDefinition.getUri(), toNodeDefinition.getParameters()));
        return toNodeDefinition;
    }

    @SneakyThrows
    private void doInterceptNodeDefinition(String flowId, String nodeId, Map<String, ?> definitionParameters, String componentId) {

        if (ObjectUtils.isEmpty(definitionParameters)) {
            return;
        }

        ComponentDefinition componentDefinition = ComponentDefinitionCache.load(componentId);
        //input variables
        Map<String, ComponentOptionDefinition> properties = componentDefinition.getProperties();
        if (!ObjectUtils.isEmpty(properties)) {
            Set<String> parameters = extractInputLogVariables(componentDefinition, definitionParameters, properties);
            if (!ObjectUtils.isEmpty(parameters)) {
                RuleNodeLogVariablesSupport.cacheInputLogVariables(flowId, nodeId, parameters);
            }
        }
        //output variables
        Map<String, ComponentOutputDefinition> outputProperties = componentDefinition.getOutputProperties();
        if (!ObjectUtils.isEmpty(outputProperties)) {
            Map.Entry<String, ComponentOutputDefinition> componentOutputDefinitionEntry = outputProperties.entrySet().iterator().next();
            String parameterKey = appendBeanPrefixIfNeed(componentDefinition.getComponent().getScheme(), componentOutputDefinitionEntry.getKey());
            if (definitionParameters.containsKey(parameterKey)) {
                List<VariableNamed> outputKeys = extractOutputKeys(definitionParameters.get(parameterKey), componentOutputDefinitionEntry.getValue());
                RuleNodeLogVariablesSupport.cacheOutputLogVariables(flowId, nodeId, outputKeys);
            }
        }
    }

    private Set<String> extractInputLogVariables(ComponentDefinition componentDefinition, Map<String, ?> parameters, Map<String, ? extends ComponentOptionExtensionDefinition> properties) {
        if (ObjectUtils.isEmpty(parameters)) {
            return Set.of();
        }
        return parameters.entrySet().stream().filter(entry -> {
            String key = removeBeanPrefixIfNeed(componentDefinition.getComponent().getScheme(), entry.getKey());
            return entry.getValue() != null && (properties.containsKey(key) && properties.get(key).isLoggable()) || isChoiceComponent(componentDefinition);
        }).flatMap(entry -> {
            List<String> expressionList = new ArrayList<>();
            SpelExpression[] spELExpressions = SpELExpressionHelper.extractIfSpELExpression(entry.getValue());
            for (SpelExpression spELExpression : spELExpressions) {
                String expression = spELExpression.getExpressionString();
                String[] expressions = (isChoiceComponent(componentDefinition)) ? ExpressionGenerator.reverseExpressionParameter(expression) : new String[]{expression};
                expressionList.addAll(Arrays.stream(expressions).filter(exp->StringUtils.startsWith(exp, "properties.") || StringUtils.startsWith(exp, "properties[")).toList());
            }
            return expressionList.stream().distinct();
        }).collect(Collectors.toSet());
    }

    @SneakyThrows
    private List<VariableNamed> extractOutputKeys(Object outputConfig, ComponentOutputDefinition definition) {
        String javaType = definition.getJavaType();
        String genericType = definition.getGenericType();

        if (javaType.equals(Map.class.getName())) {
            Object outputValue = JsonHelper.fromJSON(outputConfig.toString(), Map.class);
            return ((Map) outputValue).keySet().stream().map(obj -> VariableNamed.of(obj.toString())).toList();
        } else if (javaType.equals(List.class.getName())) {
            Class<?> clazz = ObjectUtils.isEmpty(genericType) ? String.class : Class.forName(genericType);
            Object outputValue = JsonHelper.readValue(outputConfig.toString(), new ObjectMapper().getTypeFactory().constructCollectionType(List.class, clazz));
            return ((List<?>) outputValue).stream()
                    .map(settings -> settings instanceof VariableNamed variableNamedSettings ? variableNamedSettings : VariableNamed.of(settings.toString()))
                    .toList();
        } else {
            return List.of();
        }
    }

    private String obtainComponentId(String uri, Map<String, Object> parameters) {
        String componentId = uri.startsWith(CAMEL_BEAN_SCHEMA) ? uri.replace(CAMEL_BEAN_SCHEMA, "") : uri.split(":")[0];
        return isTriggerComponent(componentId, parameters) ? CAMEL_TRIGGER : componentId;
    }

    private boolean isTriggerComponent(String componentId, Map<String, Object> parameters) {
        //fixme :  if trigger component need to be return to trigger
        return parameters.containsKey("entityConfigs") && RuleNodeNames.CAMEL_DIRECT.equals(componentId);
    }

    private boolean isChoiceComponent(ComponentDefinition componentDefinition) {
        return componentDefinition.getComponent().getScheme().equals(CAMEL_CHOICE);
    }

    private String appendBeanPrefixIfNeed(String schema, String propertyKey) {
        return schema.equals(CAMEL_BEAN) ? CAMEL_BEAN_PREFIX + propertyKey : propertyKey;
    }

    private String removeBeanPrefixIfNeed(String schema, String propertyKey) {
        return schema.equals(CAMEL_BEAN) ? propertyKey.replace(CAMEL_BEAN_PREFIX, "") : propertyKey;
    }
}
