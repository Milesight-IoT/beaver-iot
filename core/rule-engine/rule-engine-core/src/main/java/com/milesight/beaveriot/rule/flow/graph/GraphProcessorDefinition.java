package com.milesight.beaveriot.rule.flow.graph;

import lombok.Getter;
import org.apache.camel.model.OutputNode;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.spi.Metadata;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author leon
 */
@Getter
public class GraphProcessorDefinition extends ProcessorDefinition<GraphProcessorDefinition> implements OutputNode {

    @Metadata(description = "Flow Graph Config")
    private final FlowGraph flowGraph;

    public GraphProcessorDefinition(FlowGraph flowGraph) {
        this.flowGraph = flowGraph;
    }

    @Override
    public List<ProcessorDefinition<?>> getOutputs() {
        return flowGraph.getNodeDefinitions().values().stream().toList();
    }

    @Override
    public String toString() {
        return "Graph[" + flowGraph.toString() + "]";
    }

    @Override
    public String getShortName() {
        return "Graph";
    }

    @Override
    public String getLabel() {
        return getOutputs().stream().map(ProcessorDefinition::getLabel)
                .collect(Collectors.joining(",", getShortName() + "[", "]"));
    }

    public FlowGraph getFlowGraph() {
        return flowGraph;
    }

}
