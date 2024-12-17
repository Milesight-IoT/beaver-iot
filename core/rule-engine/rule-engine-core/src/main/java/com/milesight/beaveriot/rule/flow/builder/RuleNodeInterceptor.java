package com.milesight.beaveriot.rule.flow.builder;

import com.milesight.beaveriot.rule.model.flow.route.FromNode;
import com.milesight.beaveriot.rule.model.flow.route.RouteNode;
import com.milesight.beaveriot.rule.model.flow.route.base.OutputNode;

/**
 * @author leon
 */
public interface RuleNodeInterceptor {

    default FromNode interceptFrom(String flowId, FromNode fromNode) {
        return fromNode;
    }

    default OutputNode interceptOutputNode(String flowId, OutputNode outputNode) {
        return outputNode;
    }

    default RouteNode interceptRouteNode(String flowId, RouteNode routeNode) {
        return routeNode;
    }

    public class NoOpRuleNodeInterceptor implements RuleNodeInterceptor {

    }
}
