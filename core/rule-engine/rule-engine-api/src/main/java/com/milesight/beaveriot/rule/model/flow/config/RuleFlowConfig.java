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

    private List<RuleNodeConfig> nodes;

    private List<RuleEdgeConfig> edges;

    public static RuleFlowConfig createSequenceFlow(String flowId, List<RuleNodeConfig> nodes) {

        Assert.notEmpty(nodes, "nodes must not be empty");

        RuleFlowConfig flow = new RuleFlowConfig();
        flow.setFlowId(flowId);
        flow.setNodes(nodes);

        List<RuleEdgeConfig> edgeConfigs = new ArrayList<>();
        for (int i = 0; i < nodes.size() - 1; i++) {
            RuleNodeConfig source = nodes.get(i);
            RuleNodeConfig target = nodes.get(i + 1);
            edgeConfigs.add(RuleEdgeConfig.create(source.getId(), target.getId(),null));
        }
        flow.setEdges(edgeConfigs);
        return flow;
    }

}
