package com.milesight.beaveriot.rule.flow.yaml;

import com.milesight.beaveriot.rule.model.flow.yaml.FromNode;
import com.milesight.beaveriot.rule.model.flow.yaml.RouteNode;
import com.milesight.beaveriot.rule.model.flow.yaml.base.OutputNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * @author leon
 */
public class DefaultRuleNodeInterceptor implements RuleNodeInterceptor {

    private final static List<RuleNodeInterceptor> ruleNodeInterceptors;

    static {
        ServiceLoader<RuleNodeInterceptor> serviceLoaders = ServiceLoader.load(RuleNodeInterceptor.class);
        ruleNodeInterceptors = serviceLoaders.stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparingInt(RuleNodeInterceptor::getOrder))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public FromNode interceptFromNode(String flowId, FromNode fromNode) {
        for (RuleNodeInterceptor interceptor : ruleNodeInterceptors) {
            fromNode = interceptor.interceptFromNode(flowId, fromNode);
        }
        return fromNode;
    }

    @Override
    public OutputNode interceptOutputNode(String flowId, OutputNode outputNode) {
        for (RuleNodeInterceptor interceptor : ruleNodeInterceptors) {
            outputNode = interceptor.interceptOutputNode(flowId, outputNode);
        }
        return outputNode;
    }

    @Override
    public RouteNode interceptRouteNode(String flowId, RouteNode routeNode) {
        for (RuleNodeInterceptor interceptor : ruleNodeInterceptors) {
            routeNode = interceptor.interceptRouteNode(flowId, routeNode);
        }
        return routeNode;
    }
}
