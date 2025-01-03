package com.milesight.beaveriot.rule.flow.graph;

import org.apache.camel.Processor;
import org.apache.camel.Route;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.reifier.ProcessorReifier;

/**
 * @author leon
 */
public class GraphChoiceReifier extends ProcessorReifier<GraphChoiceDefinition> {

    public GraphChoiceReifier(Route route, ProcessorDefinition<?> definition) {
        super(route, GraphChoiceDefinition.class.cast(definition));
    }

    @Override
    public Processor createProcessor() throws Exception {
        return new GraphChoiceProcessor(definition.getWhenClause(), definition.getOtherwiseNodeId());
    }

}
