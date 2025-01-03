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
public class FromNodeDefinition extends AbstractNodeDefinition {

    private String uri;
    private Map<String, Object> parameters;

    public static FromNodeDefinition create(String flowId, RuleNodeConfig ruleNodeConfig, ComponentDefinition componentDefinition) {
        Map<String, Object> parameters = ComponentParameterConverter.convertParameters(ruleNodeConfig.getParameters(), componentDefinition);
        String uri = componentDefinition.generateUri(flowId, parameters);

        FromNodeDefinition nodeDefinition = new FromNodeDefinition();
        nodeDefinition.setId(ruleNodeConfig.getId());
        nodeDefinition.setNameNode(ruleNodeConfig.getName());
        nodeDefinition.setUri(uri);
        nodeDefinition.setParameters(parameters);

        return nodeDefinition;
    }
}
