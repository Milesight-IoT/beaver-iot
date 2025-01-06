package com.milesight.beaveriot.rule.flow.yaml;

import com.milesight.beaveriot.rule.flow.ComponentDefinitionCache;
import com.milesight.beaveriot.rule.model.definition.ComponentDefinition;
import com.milesight.beaveriot.rule.model.flow.ExpressionNode;
import com.milesight.beaveriot.rule.model.flow.config.RuleChoiceConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.model.flow.yaml.*;
import com.milesight.beaveriot.rule.model.flow.yaml.base.OutputNode;
import com.milesight.beaveriot.rule.support.RuleFlowIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author leon
 */
@Slf4j
public class RuleFlowYamlBuilder {

    private AtomicInteger parallelCounter = new AtomicInteger(0);
    private RuleFlowGraph ruleFlowGraph;
    private Function<String, ComponentDefinition> componentDefinitionLoader;
    private RuleNodeInterceptor ruleNodeInterceptor;
    private String flowId;
    private Stack<String> branchStack = new Stack<>();

    private RuleFlowYamlBuilder(Function<String, ComponentDefinition> componentDefinitionProvider, RuleNodeInterceptor ruleNodeInterceptor) {
        this.componentDefinitionLoader = componentDefinitionProvider;
        if (ruleNodeInterceptor == null) {
            this.ruleNodeInterceptor = new DefaultRuleNodeInterceptor();
        } else {
            this.ruleNodeInterceptor = ruleNodeInterceptor;
        }
    }

    public static RuleFlowYamlBuilder builder(Function<String, ComponentDefinition> componentDefinitionProvider, RuleNodeInterceptor ruleNodeInterceptor) {
        return new RuleFlowYamlBuilder(componentDefinitionProvider, ruleNodeInterceptor);
    }

    public static RuleFlowYamlBuilder builder(Function<String, ComponentDefinition> componentDefinitionProvider) {
        return new RuleFlowYamlBuilder(componentDefinitionProvider, null);
    }

    public static RuleFlowYamlBuilder builder() {
        return new RuleFlowYamlBuilder(ComponentDefinitionCache::load, null);
    }

    public RuleFlowYamlBuilder withRuleFlowConfig(RuleFlowConfig flowConfig) {

        Assert.notNull(flowConfig.getFlowId(), "Rule flow id must not be null");

        this.ruleFlowGraph = new RuleFlowGraph(flowConfig);
        this.flowId = flowConfig.getFlowId();
        ruleFlowGraph.initGraph();
        return this;
    }

    public RouteNode build() {

        RuleNodeConfig fromNodeConfig = (RuleNodeConfig) ruleFlowGraph.retrieveFromNode();

        List<OutputNode> outputNodes = new ArrayList<>();

        FromNode fromNode = FromNode.create(flowId, fromNodeConfig, componentDefinitionLoader.apply(fromNodeConfig.getComponentName()), outputNodes);

        retrieveOutputNodes(outputNodes, fromNodeConfig.getId(), nodes -> false);

        fromNode.setSteps(outputNodes);

        return ruleNodeInterceptor.interceptRouteNode(flowId, RouteNode.create(flowId, ruleNodeInterceptor.interceptFromNode(flowId, fromNode)));
    }

    private void retrieveOutputNodes(List<OutputNode> outputNodes, String nodeId, Predicate<Set<RuleConfig>> endBranchPredicate) {
        Set<RuleConfig> successors = ruleFlowGraph.successors(nodeId);
        if (successors.isEmpty() || endBranchPredicate.test(successors)) {
            return;
        }

        doRetrieveOutputNodes(successors, outputNodes, nodeId, endBranchPredicate);
    }

    private void doRetrieveOutputNodes(Set<RuleConfig> successors, List<OutputNode> outputNodes, String nodeId, Predicate<Set<RuleConfig>> endBranchPredicate) {
        if (isSequentialNode(successors)) {
            RuleConfig successor = successors.iterator().next();
            outputNodes.add(ruleNodeInterceptor.interceptOutputNode(flowId, RuleNode.create(flowId, (RuleNodeConfig) successor, componentDefinitionLoader.apply(successor.getComponentName()))));
            retrieveOutputNodes(outputNodes, successor.getId(), endBranchPredicate);
        } else if (isChoiceNode(successors)) {
            retrieveChoiceOutputNodes(outputNodes, successors);
        } else if (isParallelNode(successors)) {
            retrieveParallelOutputNodes(nodeId, outputNodes, successors);
        } else {
            throw new UnsupportedOperationException("not support rule node " + nodeId);
        }
    }

    protected void retrieveParallelOutputNodes(String branchStartId, List<OutputNode> outputNodes, Set<RuleConfig> successors/*, Predicate<Set<RuleConfig>> endBranchPredicate*/) {
        branchStack.push(branchStartId);
        ParallelNode.ParallelBuilder builder = ParallelNode.builder();
        String parallelNodeId = RuleFlowIdGenerator.generateNamespacedParallelId(flowId, parallelCounter.getAndIncrement());
        builder.id(parallelNodeId);
        AtomicInteger branchCounter = new AtomicInteger(0);
        for (RuleConfig successor : successors) {
            List<OutputNode> parallelNodes = new ArrayList<>();
            if (isChoiceNode(successor)) {
                retrieveChoiceOutputNodes(parallelNodes, Set.of(successor));
            } else {
                parallelNodes.add(ruleNodeInterceptor.interceptOutputNode(flowId, RuleNode.create(flowId, (RuleNodeConfig) successor, componentDefinitionLoader.apply(successor.getComponentName()))));
                retrieveOutputNodes(parallelNodes, successor.getId(), ruleConfigs -> isBranchEnd(branchStartId, ruleConfigs));
            }

            builder.then(RuleFlowIdGenerator.generateNamespacedBranchId(parallelNodeId, branchCounter.getAndIncrement()), parallelNodes);
        }
        outputNodes.addAll(builder.build().getOutputNodes());

        retrieveBranchEndOutputNodes(branchStartId, outputNodes);

        branchStack.pop();
    }


    protected void retrieveChoiceOutputNodes(List<OutputNode> outputNodes, Set<RuleConfig> successors/*, Predicate<Set<RuleConfig>> endBranchPredicate*/) {
        RuleConfig choiceNodeConfig = successors.iterator().next();
        branchStack.push(choiceNodeConfig.getId());
        ChoiceNode.ChoiceNodeBuilder builder = ChoiceNode.builder();
        builder.id(RuleFlowIdGenerator.generateNamespacedId(flowId, choiceNodeConfig.getId()));
        Set<RuleConfig> choiceSuccessors = ruleFlowGraph.successors(choiceNodeConfig.getId());
        for (RuleConfig successor : choiceSuccessors) {
            List<OutputNode> choicesNodes = new ArrayList<>();
            retrieveOutputNodes(choicesNodes, successor.getId(), ruleConfigs -> isBranchEnd(choiceNodeConfig.getId(), ruleConfigs));

            if (successor instanceof RuleChoiceConfig.RuleChoiceWhenConfig choiceWhenConfig) {
                builder.when(RuleFlowIdGenerator.generateNamespacedId(flowId, choiceWhenConfig.getId()), ExpressionNode.create(choiceWhenConfig), choicesNodes);
            } else if (successor instanceof RuleChoiceConfig.RuleChoiceOtherwiseConfig choiceOtherwiseConfig) {
                builder.otherwise(RuleFlowIdGenerator.generateNamespacedId(flowId, choiceOtherwiseConfig.getId()), choicesNodes);
            }
        }
        outputNodes.add(builder.build());

        retrieveBranchEndOutputNodes(choiceNodeConfig.getId(), outputNodes);

        branchStack.pop();
    }

    private void retrieveBranchEndOutputNodes(String branchStartId, List<OutputNode> outputNodes) {
        String detectedBranchEnd = ruleFlowGraph.detectedBranchEnd(branchStartId);
        if (detectedBranchEnd != null) {
            List<String> branchStarts = ruleFlowGraph.detectedBranchStart(detectedBranchEnd);
            // if has more than one branch start node, and the current node is the first branch start node
            if (branchStarts.size() > 1 && branchStack.firstElement().equals(branchStartId)) {
                //todo  detectedBranchEnd is seq node?
                doRetrieveOutputNodes(Set.of(ruleFlowGraph.retrieveNode(detectedBranchEnd)), outputNodes, detectedBranchEnd, (nodeConfig) -> true);
            }
        }
    }

    private boolean isBranchEnd(String branchStartId, Set<RuleConfig> ruleConfigs) {
        String detectedBranchEnd = ruleFlowGraph.detectedBranchEnd(branchStartId);
        return detectedBranchEnd != null &&
                ruleConfigs.size() == 1 && detectedBranchEnd.equals(ruleConfigs.iterator().next().getId());
    }

    private boolean isParallelNode(Set<RuleConfig> successors) {
        return successors.size() > 1;
    }

    private boolean isChoiceNode(Set<RuleConfig> successors) {
        return successors.size() == 1 && isChoiceNode(successors.iterator().next());
    }

    private boolean isChoiceNode(RuleConfig successor) {
        return RuleConfig.COMPONENT_CHOICE.equals(successor.getComponentName());
    }

    private boolean isSequentialNode(Set<RuleConfig> successors) {
        return successors.size() == 1 && !RuleConfig.COMPONENT_CHOICE.equals(successors.iterator().next().getComponentName());
    }
}
