package com.milesight.beaveriot.rule.model.flow.yaml;

import com.milesight.beaveriot.rule.model.definition.ComponentDefinition;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.model.flow.yaml.base.NodeId;
import com.milesight.beaveriot.rule.model.flow.yaml.base.OutputNode;
import com.milesight.beaveriot.rule.support.ComponentParameterConverter;
import com.milesight.beaveriot.rule.support.RuleFlowIdGenerator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author leon
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RuleNode implements OutputNode {

    private InnerRuleNode to;

    public static RuleNode create(String id, String uri, Map<String, Object> parameters) {
        return new RuleNode(new InnerRuleNode(id, uri, parameters));
    }

    public static OutputNode create(String flowId, RuleNodeConfig ruleConfig, ComponentDefinition componentDefinition) {

        String generatedId = RuleFlowIdGenerator.generateNamespacedId(flowId, ruleConfig.getId());

        Map<String, Object> parameters = ComponentParameterConverter.convertParameters(ruleConfig.getParameters(), componentDefinition);

        return RuleNode.create(generatedId, componentDefinition.generateUri(generatedId, ruleConfig, parameters), parameters);
    }

    @Override
    public String getId() {
        return to.getId();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InnerRuleNode implements NodeId {

        private String id;
        private String uri;
        private Map<String, Object> parameters;

    }
}
