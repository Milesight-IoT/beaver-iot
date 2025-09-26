package com.milesight.beaveriot.rule.model.flow.config;

import lombok.Data;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leon
 */
@Data
public class RuleFlowConfig {

    private String flowId;

    private String name;

    private String fromNodeId;

    private List<RuleNodeConfig> nodes;

    private List<RuleEdgeConfig> edges;

    private RuleFlowConfigMetadata metadata;

    private List<RuleConfig> initializedNodes = new ArrayList<>();

    public void initialize() {
        Assert.notNull(flowId, "flowId must not be empty");

        nodes.stream().forEach(node -> {
            if (node.getComponentName().equals(RuleConfig.COMPONENT_CHOICE)) {
                initializedNodes.add(RuleChoiceConfig.create(node));
            } else {
                initializedNodes.add(node);
            }
        });
    }

    public static RuleFlowConfig create(String flowId) {
        RuleFlowConfig flow = new RuleFlowConfig();
        flow.setFlowId(flowId);
        return flow;
    }

    public static RuleFlowConfig createSequenceFlow(String flowId, List<RuleNodeConfig> nodes) {

        Assert.notEmpty(nodes, "nodes must not be empty");

        RuleFlowConfig flow = new RuleFlowConfig();
        flow.setFlowId(flowId);
        flow.setNodes(nodes);

        List<RuleEdgeConfig> edgeConfigs = new ArrayList<>();
        for (int i = 0; i < nodes.size() - 1; i++) {
            RuleConfig source = nodes.get(i);
            RuleConfig target = nodes.get(i + 1);
            edgeConfigs.add(RuleEdgeConfig.create(source.getId(), target.getId(), null));
        }
        flow.setEdges(edgeConfigs);
        return flow;
    }

}
