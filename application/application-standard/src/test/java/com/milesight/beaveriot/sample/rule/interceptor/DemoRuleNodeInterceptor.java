package com.milesight.beaveriot.sample.rule.interceptor;

import com.milesight.beaveriot.rule.flow.yaml.RuleNodeInterceptor;
import com.milesight.beaveriot.rule.model.flow.yaml.FromNode;
import com.milesight.beaveriot.rule.model.flow.yaml.RouteNode;
import com.milesight.beaveriot.rule.model.flow.yaml.base.OutputNode;

/**
 * @author leon
 */
public class DemoRuleNodeInterceptor implements RuleNodeInterceptor {
    @Override
    public FromNode interceptFromNode(String flowId, FromNode fromNode) {
        //modify fromNode
        return RuleNodeInterceptor.super.interceptFromNode(flowId, fromNode);
    }

    @Override
    public OutputNode interceptOutputNode(String flowId, OutputNode outputNode) {
        //modify outputNode
        return RuleNodeInterceptor.super.interceptOutputNode(flowId, outputNode);
    }

    @Override
    public RouteNode interceptRouteNode(String flowId, RouteNode routeNode) {
        return RuleNodeInterceptor.super.interceptRouteNode(flowId, routeNode);
    }
}
