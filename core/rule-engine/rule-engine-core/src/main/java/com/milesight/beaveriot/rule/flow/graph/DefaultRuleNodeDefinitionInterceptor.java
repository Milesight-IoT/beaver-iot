package com.milesight.beaveriot.rule.flow.graph;

import com.milesight.beaveriot.rule.RuleNodeDefinitionInterceptor;
import com.milesight.beaveriot.rule.model.flow.route.AbstractNodeDefinition;
import com.milesight.beaveriot.rule.model.flow.route.ChoiceNodeDefinition;
import com.milesight.beaveriot.rule.model.flow.route.FromNodeDefinition;
import com.milesight.beaveriot.rule.model.flow.route.ToNodeDefinition;
import org.apache.camel.model.ProcessorDefinition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * @author leon
 */
public class DefaultRuleNodeDefinitionInterceptor implements RuleNodeDefinitionInterceptor {

    private final static List<RuleNodeDefinitionInterceptor> ruleNodeDefinitionInterceptors;

    static {
        ServiceLoader<RuleNodeDefinitionInterceptor> serviceLoaders = ServiceLoader.load(RuleNodeDefinitionInterceptor.class);
        ruleNodeDefinitionInterceptors = serviceLoaders.stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparingInt(RuleNodeDefinitionInterceptor::getOrder))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public FromNodeDefinition interceptFromNodeDefinition(String flowId, FromNodeDefinition fromNode) {
        for (RuleNodeDefinitionInterceptor interceptor : ruleNodeDefinitionInterceptors) {
            fromNode = interceptor.interceptFromNodeDefinition(flowId, fromNode);
        }
        return fromNode;
    }

    @Override
    public ToNodeDefinition interceptToNodeDefinition(String flowId, ToNodeDefinition toNodeDefinition) {
        for (RuleNodeDefinitionInterceptor interceptor : ruleNodeDefinitionInterceptors) {
            toNodeDefinition = interceptor.interceptToNodeDefinition(flowId, toNodeDefinition);
        }
        return toNodeDefinition;
    }

    @Override
    public ProcessorDefinition postProcessNodeDefinition(String flowId, AbstractNodeDefinition nodeDefinition) {
        for (RuleNodeDefinitionInterceptor interceptor : ruleNodeDefinitionInterceptors) {
            ProcessorDefinition processDefinition = interceptor.postProcessNodeDefinition(flowId, nodeDefinition);
            if (processDefinition != null) {
                return processDefinition;
            }
        }
        return null;
    }

    @Override
    public ChoiceNodeDefinition interceptChoiceNodeDefinition(String flowId, ChoiceNodeDefinition choiceNodeDefinition) {
        for (RuleNodeDefinitionInterceptor interceptor : ruleNodeDefinitionInterceptors) {
            choiceNodeDefinition = interceptor.interceptChoiceNodeDefinition(flowId, choiceNodeDefinition);
        }
        return choiceNodeDefinition;
    }
}
