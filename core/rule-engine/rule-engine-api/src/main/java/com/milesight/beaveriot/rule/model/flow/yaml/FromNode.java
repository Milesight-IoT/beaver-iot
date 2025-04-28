package com.milesight.beaveriot.rule.model.flow.yaml;

import com.milesight.beaveriot.rule.model.definition.ComponentDefinition;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.model.flow.yaml.base.MultiOutputNode;
import com.milesight.beaveriot.rule.model.flow.yaml.base.NodeId;
import com.milesight.beaveriot.rule.model.flow.yaml.base.OutputNode;
import com.milesight.beaveriot.rule.support.ComponentParameterConverter;
import com.milesight.beaveriot.rule.support.RuleFlowIdGenerator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author leon
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FromNode implements NodeId {

    private String id;
    private String uri;
    private Map<String, Object> parameters;
    private List<OutputNode> steps;

    public static FromNode create(String id, String uri, Map<String, Object> parameters, List<OutputNode> steps) {
        List<OutputNode> stepsList = steps.stream().flatMap(step -> {
            if (step instanceof MultiOutputNode multiOutputNode) {
                return multiOutputNode.getOutputNodes().stream();
            } else {
                return Stream.of(step);
            }
        }).toList();
        return new FromNode(id, uri, parameters, stepsList);
    }

    public static FromNode create(String flowId, RuleNodeConfig fromNodeConfig, ComponentDefinition componentDefinition, List<OutputNode> steps) {

        Map<String, Object> parameters = ComponentParameterConverter.convertParameters(fromNodeConfig.getParameters(), componentDefinition);

        String generatedId = RuleFlowIdGenerator.generateNamespacedId(flowId, fromNodeConfig.getId());

        return FromNode.create(generatedId, componentDefinition.generateUri(flowId, fromNodeConfig, parameters),  parameters, steps);
    }
}
