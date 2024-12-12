package com.milesight.beaveriot.rule.flow.builder;

import com.milesight.beaveriot.rule.flow.ComponentDefinitionCache;
import com.milesight.beaveriot.rule.model.definition.ComponentDefinition;
import com.milesight.beaveriot.rule.model.flow.config.RuleChoiceConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.model.flow.dsl.*;
import com.milesight.beaveriot.rule.model.flow.dsl.base.OutputNode;
import com.milesight.beaveriot.rule.support.RuleFlowIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author leon
 */
@Slf4j
public class RuleFlowYamlBuilder {

    private AtomicInteger parallelCounter = new AtomicInteger(0);
    private RuleFlowGraph ruleFlowGraph;
    private RuleFlowConfig ruleFlow;
    private Function<String, ComponentDefinition> componentDefinitionLoader;

    private RuleFlowYamlBuilder(Function<String, ComponentDefinition> componentDefinitionProvider) {
        this.componentDefinitionLoader = componentDefinitionProvider;
    }

    public static RuleFlowYamlBuilder builder(Function<String, ComponentDefinition> componentDefinitionProvider) {
        return new RuleFlowYamlBuilder(componentDefinitionProvider);
    }

    public static RuleFlowYamlBuilder builder() {
        return new RuleFlowYamlBuilder(ComponentDefinitionCache::load);
    }

    public RuleFlowYamlBuilder withRuleFlowConfig(RuleFlowConfig flowConfig) {

        Assert.notNull(flowConfig.getFlowId(), "Rule flow id must not be null");

        RuleFlowGraph ruleFlowGraph = new RuleFlowGraph(flowConfig);
        ruleFlowGraph.initGraph();
        this.ruleFlow = flowConfig;
        this.ruleFlowGraph = ruleFlowGraph;
        return this;
    }

    public RouteNode build() {

        RuleNodeConfig fromNodeConfig = (RuleNodeConfig) ruleFlowGraph.retrieveFromNode();

        List<OutputNode> outputNodes = new ArrayList<>();

        FromNode fromNode = FromNode.create(ruleFlow.getFlowId(), fromNodeConfig, componentDefinitionLoader.apply(fromNodeConfig.getComponentId()), outputNodes);

        retrieveOutputNodes(outputNodes, fromNodeConfig.getId(), nodes -> false);

        fromNode.setSteps(outputNodes);

        return RouteNode.create(ruleFlow.getFlowId(), fromNode);
    }

    private void retrieveOutputNodes(List<OutputNode> outputNodes, String nodeId, Predicate<Set<RuleConfig>> endBranchPredicate) {
        Set<RuleConfig> successors = ruleFlowGraph.successors(nodeId);
        if (successors.isEmpty() || endBranchPredicate.test(successors)) {
            return;
        }

        if (isSequentialNode(successors)) {
            RuleConfig successor = successors.iterator().next();
            //todo check rule node type
            outputNodes.add(RuleNode.create(ruleFlow.getFlowId(), (RuleNodeConfig) successor, componentDefinitionLoader.apply(successor.getComponentId())));
            retrieveOutputNodes(outputNodes, successor.getId(), nodes -> false);
        } else if (isChoiceNode(successors)) {
            retrieveChoiceOutputNodes(outputNodes, successors);
        } else if (isParallelNode(successors)) {
            retrieveParallelOutputNodes(outputNodes, successors);
        } else {
            throw new UnsupportedOperationException("not support rule node " + nodeId);
        }
    }

    protected void retrieveParallelOutputNodes(List<OutputNode> outputNodes, Set<RuleConfig> successors) {
        ParallelNode.ParallelBuilder builder = ParallelNode.builder();
        String flowId = RuleFlowIdGenerator.generateNamespacedParallelId(ruleFlow.getFlowId(), parallelCounter.getAndIncrement());
        builder.id(flowId);
        AtomicReference<RuleConfig> endChoiceNode = new AtomicReference<>();
        AtomicInteger branchCounter = new AtomicInteger(0);
        for (RuleConfig successor : successors) {
            List<OutputNode> parallelNodes = new ArrayList<>();
            //todo check if choice
            parallelNodes.add(RuleNode.create(ruleFlow.getFlowId(), (RuleNodeConfig) successor, componentDefinitionLoader.apply(successor.getComponentId())));
            retrieveOutputNodes(parallelNodes, successor.getId(), nodes -> onEndBranch(nodes, endChoiceNode));
            builder.then(RuleFlowIdGenerator.generateNamespacedBranchId(flowId, branchCounter.getAndIncrement()), parallelNodes);
        }
        outputNodes.addAll(builder.build().getOutputNodes());
        if (endChoiceNode.get() != null) {
            RuleNodeConfig ruleNodeConfig = (RuleNodeConfig) endChoiceNode.get();
            outputNodes.add(RuleNode.create(ruleFlow.getFlowId(), ruleNodeConfig, componentDefinitionLoader.apply(ruleNodeConfig.getComponentId())));
            retrieveOutputNodes(outputNodes, endChoiceNode.get().getId(), node -> false);
        }
    }

    protected void retrieveChoiceOutputNodes(List<OutputNode> outputNodes, Set<RuleConfig> successors) {
        RuleConfig choiceNodeConfig = successors.iterator().next();
        ChoiceNode.ChoiceNodeBuilder builder = ChoiceNode.builder();
        builder.id(RuleFlowIdGenerator.generateNamespacedId(ruleFlow.getFlowId(), choiceNodeConfig.getId()));
        Set<RuleConfig> choiceSuccessors = ruleFlowGraph.successors(choiceNodeConfig.getId());
        AtomicReference<RuleConfig> endChoiceNode = new AtomicReference<>();
        for (RuleConfig successor : choiceSuccessors) {
            List<OutputNode> choicesNodes = new ArrayList<>();
            retrieveOutputNodes(choicesNodes, successor.getId(), nodes -> onEndBranch(nodes, endChoiceNode));

            if (successor instanceof RuleChoiceConfig.RuleChoiceWhenConfig choiceWhenConfig) {
                builder.when(choiceWhenConfig.getId(), ExpressionNode.create(choiceWhenConfig), choicesNodes);
            } else if (successor instanceof RuleChoiceConfig.RuleChoiceOtherwiseConfig choiceOtherwiseConfig) {
                builder.otherwise(choiceOtherwiseConfig.getId(), choicesNodes);
            }
        }
        outputNodes.add(builder.build());

        if (endChoiceNode.get() != null) {
            RuleNodeConfig ruleNodeConfig = (RuleNodeConfig) endChoiceNode.get();
            outputNodes.add(RuleNode.create(ruleFlow.getFlowId(), ruleNodeConfig, componentDefinitionLoader.apply(ruleNodeConfig.getComponentId())));
            retrieveOutputNodes(outputNodes, endChoiceNode.get().getId(), node -> false);
        }
    }

    private Boolean onEndBranch(Set<RuleConfig> nodes, AtomicReference<RuleConfig> endChoiceNode) {
        if (nodes.size() == 1 && ruleFlowGraph.inDegree(nodes.iterator().next()) > 1) {
            endChoiceNode.set(nodes.iterator().next());
            return true;
        } else {
            return false;
        }
    }

    private boolean isParallelNode(Set<RuleConfig> successors) {
        return successors.size() > 1;
    }

    private boolean isChoiceNode(Set<RuleConfig> successors) {
        return successors.size() == 1 && RuleConfig.COMPONENT_CHOICE.equals(successors.iterator().next().getComponentId());
    }

    private boolean isSequentialNode(Set<RuleConfig> successors) {
        return successors.size() == 1 && !RuleConfig.COMPONENT_CHOICE.equals(successors.iterator().next().getComponentId());
    }
}
