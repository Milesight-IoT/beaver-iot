package com.milesight.beaveriot.rule.flow.graph;

import lombok.Getter;
import lombok.Setter;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.WhenDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author leon
 */
@Getter
@Setter
public class GraphChoiceDefinition extends ProcessorDefinition<GraphChoiceDefinition> {

    private Map<String, WhenDefinition> whenClause = new LinkedHashMap<>();
    private String otherwiseNodeId;

    @Override
    public String getShortName() {
        return "GraphChoice";
    }

    @Override
    public String toString() {
        return "GraphChoice[" + getLabel() + "]";
    }

    @Override
    public String getLabel() {
        return whenClause.keySet().stream()
                .collect(Collectors.joining(",", getShortName() + "[", "," + otherwiseNodeId + "]"));
    }

    @Override
    public List<ProcessorDefinition<?>> getOutputs() {
        return List.of();
    }
}
