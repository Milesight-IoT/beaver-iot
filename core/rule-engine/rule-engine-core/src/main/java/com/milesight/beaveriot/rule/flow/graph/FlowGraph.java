package com.milesight.beaveriot.rule.flow.graph;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.milesight.beaveriot.rule.flow.ComponentDefinitionCache;
import com.milesight.beaveriot.rule.model.definition.ComponentDefinition;
import com.milesight.beaveriot.rule.model.flow.config.*;
import com.milesight.beaveriot.rule.model.flow.node.AbstractNodeDefinition;
import com.milesight.beaveriot.rule.model.flow.node.ChoiceNodeDefinition;
import com.milesight.beaveriot.rule.model.flow.node.FromNodeDefinition;
import com.milesight.beaveriot.rule.model.flow.node.ToNodeDefinition;
import lombok.Getter;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author leon
 */
@Getter
public class FlowGraph {

    protected final String flowId;
    protected final MutableGraph<String> graphStructure;
    protected final FromDefinition fromDefinition;
    protected final Map<String, ProcessorDefinition<?>> nodeDefinitions;

    protected FlowGraph(String flowId, MutableGraph<String> graphStructure, FromDefinition fromDefinition, Map<String, ProcessorDefinition<?>> nodeDefinitions) {
        this.graphStructure = graphStructure;
        this.fromDefinition = fromDefinition;
        this.nodeDefinitions = nodeDefinitions;
        this.flowId = flowId;
    }

    public static FlowGraphBuilder builder(String flowId) {
        return new FlowGraphBuilder(flowId);
    }

    public static FlowGraphBuilder builder(RuleFlowConfig ruleFlowConfig) {
        return new FlowGraphBuilder(ruleFlowConfig);
    }

    public static FlowGraphBuilder builder(RuleFlowConfig ruleFlowConfig, Function<String, ComponentDefinition> componentDefinitionLoader) {
        return new FlowGraphBuilder(ruleFlowConfig, componentDefinitionLoader);
    }

    public static class FlowGraphBuilder {
        private Map<String, AbstractNodeDefinition> nodeDefinitionConfigs = new LinkedHashMap<>();
        private MutableGraph<String> graphStructure = GraphBuilder.directed().build();
        private Map<String, List<String>> choiceWhenEdges = new ConcurrentHashMap<>();
        private final RuleFlowConfig ruleFlowConfig;
        private final Function<String, ComponentDefinition> componentDefinitionLoader;

        public FlowGraphBuilder(String flowId) {
            this(RuleFlowConfig.create(flowId), null);
        }

        public FlowGraphBuilder(RuleFlowConfig ruleFlowConfig) {
            this(ruleFlowConfig, null);
        }

        public FlowGraphBuilder(RuleFlowConfig ruleFlowConfig, Function<String, ComponentDefinition> componentDefinitionLoader) {
            this.ruleFlowConfig = ruleFlowConfig;
            this.componentDefinitionLoader = componentDefinitionLoader == null ? ComponentDefinitionCache::load : componentDefinitionLoader;
        }

        public FlowGraph build() {

            ruleFlowConfig.initialize();

            ruleFlowConfig.getEdges().forEach(this::populateGraphEdges);

            FromNodeDefinition fromNodeDefinition = populateGraphFromNodeDefinitions();

            ruleFlowConfig.getInitializedNodes().forEach(this::populateGraphNodeDefinitions);

            FromDefinition fromDefinition = RouteDefinitionConverter.convertFromDefinition(ruleFlowConfig.getFlowId(), fromNodeDefinition);

            Map<String, ProcessorDefinition<?>> processorDefinitions = new LinkedHashMap<>();
            nodeDefinitionConfigs.entrySet().stream()
                    .filter(entry -> !(entry.getKey().equals(fromNodeDefinition.getId())))
                    .forEach(entry -> processorDefinitions.put(entry.getKey(), RouteDefinitionConverter.convertProcessorDefinition(ruleFlowConfig.getFlowId(), entry.getValue(), choiceWhenEdges)));

            return new FlowGraph(ruleFlowConfig.getFlowId(), graphStructure, fromDefinition, processorDefinitions);
        }

        private FromNodeDefinition populateGraphFromNodeDefinitions() {
            String fromNodeId = retrieveFromNodeId();
            RuleNodeConfig ruleNodeConfig = ruleFlowConfig.getNodes()
                    .stream()
                    .filter(node -> node.getId().equals(fromNodeId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("From node not found"));
            return convertToFromNodeDefinition(ruleNodeConfig);
        }

        private String retrieveFromNodeId() {
            for (String node : graphStructure.nodes()) {
                if (graphStructure.inDegree(node) == 0) {
                    return node;
                }
            }
            throw new IllegalStateException("No start node found");
        }

        private void populateGraphEdges(RuleEdgeConfig edge) {
            if (StringUtils.hasText(edge.getSourceHandle())) {
                graphStructure.putEdge(edge.getSource(), edge.getTarget());
                List<String> whenOutputIds = choiceWhenEdges.computeIfAbsent(edge.getSourceHandle(), k -> new ArrayList<>());
                whenOutputIds.add(edge.getTarget());
            } else {
                graphStructure.putEdge(edge.getSource(), edge.getTarget());
            }
        }

        private void populateGraphNodeDefinitions(RuleConfig nodeConfig) {
            AbstractNodeDefinition abstractNodeDefinition = convertToNodeDefinition(nodeConfig);
            nodeDefinitionConfigs.put(nodeConfig.getId(), abstractNodeDefinition);
            graphStructure.addNode(abstractNodeDefinition.getId());
        }

        private FromNodeDefinition convertToFromNodeDefinition(RuleConfig nodeConfig) {
            ComponentDefinition componentDefinition = componentDefinitionLoader.apply(nodeConfig.getComponentName());
            return FromNodeDefinition.create(ruleFlowConfig.getFlowId(), (RuleNodeConfig) nodeConfig, componentDefinition);
        }

        private AbstractNodeDefinition convertToNodeDefinition(RuleConfig nodeConfig) {
            ComponentDefinition componentDefinition = componentDefinitionLoader.apply(nodeConfig.getComponentName());
            if (nodeConfig instanceof RuleChoiceConfig ruleChoiceConfig) {
                return ChoiceNodeDefinition.create(ruleChoiceConfig);
            } else if (nodeConfig instanceof RuleNodeConfig ruleNodeConfig) {
                return ToNodeDefinition.create(ruleNodeConfig, componentDefinition);
            } else {
                throw new IllegalArgumentException("Unsupported node type: " + nodeConfig.getClass().getName());
            }
        }
    }
}
