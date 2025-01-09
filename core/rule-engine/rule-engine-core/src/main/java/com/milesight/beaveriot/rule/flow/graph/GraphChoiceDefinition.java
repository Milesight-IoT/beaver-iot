package com.milesight.beaveriot.rule.flow.graph;

import lombok.Getter;
import lombok.Setter;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.WhenDefinition;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author leon
 */
@Getter
@Setter
public class GraphChoiceDefinition extends ProcessorDefinition<GraphChoiceDefinition> {

    private List<Pair<String, WhenDefinition>> whenClause = new ArrayList<>();
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
        return whenClause.stream()
                .map(Pair::getLeft)
                .collect(Collectors.joining(",", getShortName() + "[", "," + otherwiseNodeId + "]"));
    }

    @Override
    public List<ProcessorDefinition<?>> getOutputs() {
        return List.of();
    }
}
