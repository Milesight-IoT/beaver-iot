package com.milesight.beaveriot.rule.flow.yaml;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.milesight.beaveriot.rule.model.flow.config.RuleChoiceConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author leon
 */
public class RuleFlowGraph {

    private RuleFlowConfig ruleFlowConfig;
    private MutableGraph<String> mutableGraph;
    private Map<String, RuleConfig> ruleNodeCache = new LinkedHashMap<>();
    private Map<String, String> branchEndCache;

    public RuleFlowGraph(RuleFlowConfig ruleFlowConfig) {
        this.ruleFlowConfig = ruleFlowConfig;
        this.mutableGraph = GraphBuilder.directed().build();
    }

    public void initGraph() {

        Assert.notNull(ruleFlowConfig, "ruleFlowConfig is null");
        Assert.notEmpty(ruleFlowConfig.getEdges(), "Edges is null");
        Assert.notEmpty(ruleFlowConfig.getNodes(), "Nodes is null");

        ruleFlowConfig.getEdges().forEach(edge -> {
            if (StringUtils.hasText(edge.getSourceHandle())) {
                mutableGraph.putEdge(edge.getSource(), edge.getSourceHandle());
                mutableGraph.putEdge(edge.getSourceHandle(), edge.getTarget());
            } else {
                mutableGraph.putEdge(edge.getSource(), edge.getTarget());
            }
        });
        ruleFlowConfig.getNodes().forEach(node -> {
            ruleNodeCache.put(node.getId(), node);
            mutableGraph.addNode(node.getId());

            //choice edge and nodes init
            if (node.getComponentName().equals(RuleConfig.COMPONENT_CHOICE)) {
                RuleChoiceConfig ruleChoiceConfig = RuleChoiceConfig.create(node);
                Assert.notNull(ruleChoiceConfig, "Invalid choice config, parameters is null");

                ruleChoiceConfig.getWhen().forEach(when -> {
                    ruleNodeCache.put(when.getId(), when);
                });

                RuleChoiceConfig.RuleChoiceOtherwiseConfig otherwise = ruleChoiceConfig.getOtherwise();
                if (otherwise != null) {
                    ruleNodeCache.put(otherwise.getId(), otherwise);
                }
            }
        });
    }

    public String detectedBranchEnd(String branchStartId) {
        if (branchEndCache == null) {
            initBranchEndCache();
        }
        return branchEndCache.get(branchStartId);
    }

    public List<String> detectedBranchStart(String branchEndId) {
        if (branchEndCache == null) {
            initBranchEndCache();
        }
        return branchEndCache.entrySet().stream()
                .filter(entry -> branchEndId.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    private void initBranchEndCache() {
        branchEndCache = new LinkedHashMap<>();
        List<String> branchStartNodes = new ArrayList<>();
        //detect branch start node
        for (String node : mutableGraph.nodes()) {
            if (mutableGraph.successors(node).size() > 1) {
                branchStartNodes.add(node);
            }
        }
        //detect branch end node
        List<String> branchEndNodes = new ArrayList<>();
        for (String node : mutableGraph.nodes()) {
            if (mutableGraph.inDegree(node) > 1) {
                branchEndNodes.add(node);
            }
        }
        //build branch end cache
        for (String branchStartNode : branchStartNodes) {
            for (String branchEndNode : branchEndNodes) {
                if (branchStartNode.equals(branchEndNode)) {
                    continue;
                }
                boolean reachable = isReachable(branchStartNode, branchEndNode);
                if (reachable) {
                    branchEndCache.put(branchStartNode, branchEndNode);
                    break;
                }
            }
        }
        branchStartNodes.forEach(branchStartNode -> {
            if (!branchEndCache.containsKey(branchStartNode)) {
                branchEndCache.put(branchStartNode, null);
            }
        });
    }

    public Set<RuleConfig> successors(String nodeId) {
        Set<String> successors = mutableGraph.successors(nodeId);
        return CollectionUtils.isEmpty(successors) ? Collections.emptySet() : successors.stream().map(ruleNodeCache::get).collect(Collectors.toSet());
    }

    public int inDegree(RuleConfig ruleNodeConfig) {
        return mutableGraph.inDegree(ruleNodeConfig.getId());
    }

    public RuleConfig retrieveFromNode() {
        for (String node : mutableGraph.nodes()) {
            if (mutableGraph.inDegree(node) == 0) {
                return ruleNodeCache.get(node);
            }
        }
        throw new IllegalStateException("No start node found");
    }

    public boolean isReachable(String startId, String endId) {
        if (startId.equals(endId)) {
            return true;
        }
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(startId);
        visited.add(startId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String neighbor : mutableGraph.successors(current)) {
                if (!visited.contains(neighbor)) {
                    if (neighbor.equals(endId)) {
                        return true;
                    }
                    queue.add(neighbor);
                    visited.add(neighbor);
                }
            }
        }
        return false;
    }

    public RuleConfig retrieveNode(String nodeId) {
        return ruleNodeCache.get(nodeId);
    }
}
