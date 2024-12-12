package com.milesight.beaveriot.rule.flow.builder;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.milesight.beaveriot.rule.model.flow.config.RuleChoiceConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * @author leon
 */
public class RuleFlowGraph {

    private RuleFlowConfig ruleFlowConfig;
    private MutableGraph<String> ruleFlowGraph;
    private Map<String, RuleConfig> ruleNodeCache = new LinkedHashMap<>();

    public RuleFlowGraph(RuleFlowConfig ruleFlowConfig) {
        this.ruleFlowConfig = ruleFlowConfig;
        this.ruleFlowGraph = GraphBuilder.directed().build();
    }

    public void initGraph() {

        Assert.notNull(ruleFlowConfig, "ruleFlowConfig is null");
        Assert.notEmpty(ruleFlowConfig.getEdges(), "Edges is null");
        Assert.notEmpty(ruleFlowConfig.getNodes(), "Nodes is null");

        ruleFlowConfig.getEdges().forEach(edge -> ruleFlowGraph.putEdge(edge.getSource(), edge.getTarget()));

        ruleFlowConfig.getNodes().forEach(node -> {
            ruleNodeCache.put(node.getId(), node);
            ruleFlowGraph.addNode(node.getId());

            //choice edge and nodes init
            if (node.getComponentId().equals(RuleConfig.COMPONENT_CHOICE)) {
                RuleChoiceConfig ruleChoiceConfig = RuleChoiceConfig.create(node.getParameters());
                ruleChoiceConfig.getWhen().forEach(when -> {
                    ruleNodeCache.put(when.getId(), when);
                    ruleFlowGraph.putEdge(node.getId(), when.getId());
                });

                RuleChoiceConfig.RuleChoiceOtherwiseConfig otherwise = ruleChoiceConfig.getOtherwise();
                if (otherwise != null) {
                    ruleNodeCache.put(otherwise.getId(), otherwise);
                    ruleFlowGraph.putEdge(node.getId(), otherwise.getId());
                }
            }
        });
    }

    public Set<RuleConfig> successors(String nodeId) {
        Set<String> successors = ruleFlowGraph.successors(nodeId);
        return CollectionUtils.isEmpty(successors) ? Collections.emptySet() : successors.stream().map(ruleNodeCache::get).collect(Collectors.toSet());
    }

    public int inDegree(RuleConfig ruleNodeConfig) {
        return ruleFlowGraph.inDegree(ruleNodeConfig.getId());
    }

    public RuleConfig retrieveFromNode() {
        for (String node : ruleFlowGraph.nodes()) {
            if (ruleFlowGraph.inDegree(node) == 0) {
                return ruleNodeCache.get(node);
            }
        }
        throw new IllegalStateException("No start node found");
    }
}
