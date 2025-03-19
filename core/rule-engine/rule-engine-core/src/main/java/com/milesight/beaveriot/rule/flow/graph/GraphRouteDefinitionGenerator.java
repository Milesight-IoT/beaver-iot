package com.milesight.beaveriot.rule.flow.graph;

import com.milesight.beaveriot.rule.RuleNodeDefinitionInterceptor;
import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import jakarta.annotation.Nullable;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.RouteDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leon
 */
public class GraphRouteDefinitionGenerator {

    private GraphRouteDefinitionGenerator() {
    }

    public static List<RouteDefinition> generateRouteDefinition(RuleFlowConfig ruleFlowConfig, @Nullable RuleNodeDefinitionInterceptor ruleNodeDefinitionInterceptor) {

        FlowGraph.FlowGraphBuilder graphBuilder = FlowGraph.builder(ruleFlowConfig);
        if (ruleNodeDefinitionInterceptor != null) {
            graphBuilder.ruleNodeDefinitionInterceptor(ruleNodeDefinitionInterceptor);
        }
        FlowGraph flowGraph = graphBuilder.build();
        GraphProcessorDefinition graphProcessorDefinition = new GraphProcessorDefinition(flowGraph);

        FromDefinition fromDefinition = graphProcessorDefinition.getFlowGraph().getFromDefinition();
        graphProcessorDefinition.setId(flowGraph.getFlowId());

        RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setId(graphProcessorDefinition.getId());
        routeDefinition.from(fromDefinition.getUri()).addOutput(graphProcessorDefinition);
        routeDefinition.getInput().setId(fromDefinition.getId());
        routeDefinition.setDescription(ruleFlowConfig.getName());

        List<RouteDefinition> routeDefinitions = new ArrayList<>();
        routeDefinitions.add(routeDefinition);
        return routeDefinitions;
    }

}
