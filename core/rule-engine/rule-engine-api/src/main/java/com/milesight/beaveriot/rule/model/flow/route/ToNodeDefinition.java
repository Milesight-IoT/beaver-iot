package com.milesight.beaveriot.rule.model.flow.route;

import com.milesight.beaveriot.rule.model.definition.ComponentDefinition;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.support.ComponentParameterConverter;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @author leon
 */
@Getter
@Setter
public class ToNodeDefinition extends AbstractNodeDefinition {

    private String uri;
    private Map<String, Object> parameters;

    protected ToNodeDefinition(String uri, Map<String, Object> parameters) {
        this.uri = uri;
        this.parameters = parameters;
    }

    public static ToNodeDefinition create(RuleNodeConfig ruleNodeConfig, ComponentDefinition componentDefinition) {

         Map<String, Object> parameters = ComponentParameterConverter.convertParameters(ruleNodeConfig.getParameters(), componentDefinition);
        String uri = componentDefinition.generateUri(ruleNodeConfig.getId(), ruleNodeConfig, parameters);
        ToNodeDefinition nodeDefinition = new ToNodeDefinition(uri, parameters);
        nodeDefinition.setId(ruleNodeConfig.getId());
        nodeDefinition.setNameNode(ruleNodeConfig.getName());
        nodeDefinition.setParameters(parameters);

        return nodeDefinition;
    }

}
