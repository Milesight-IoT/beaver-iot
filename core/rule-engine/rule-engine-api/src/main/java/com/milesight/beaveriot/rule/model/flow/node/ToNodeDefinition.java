package com.milesight.beaveriot.rule.model.flow.node;

import com.milesight.beaveriot.rule.model.definition.ComponentDefinition;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.support.ComponentParameterConverter;
import lombok.Getter;

import java.util.Map;

/**
 * @author leon
 */
@Getter
public class ToNodeDefinition extends AbstractNodeDefinition {

    private final String uri;
    private final Map<String, Object> parameters;

    protected ToNodeDefinition(String uri, Map<String, Object> parameters) {
        this.uri = uri;
        this.parameters = parameters;
    }

    public static ToNodeDefinition create(RuleNodeConfig ruleNodeConfig, ComponentDefinition componentDefinition) {

        Map<String, Object> parameters = ComponentParameterConverter.convertParameters(ruleNodeConfig.getParameters(), componentDefinition);
        String uri = componentDefinition.generateUri(ruleNodeConfig.getId(), parameters);
        ToNodeDefinition nodeDefinition = new ToNodeDefinition(uri, parameters);
        nodeDefinition.setId(ruleNodeConfig.getId());
        nodeDefinition.setNameNode(ruleNodeConfig.getName());

        return nodeDefinition;
    }

}
