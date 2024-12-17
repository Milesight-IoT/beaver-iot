package com.milesight.beaveriot.rule;

import com.milesight.beaveriot.rule.model.flow.route.FromNode;
import com.milesight.beaveriot.rule.model.flow.route.RouteNode;
import com.milesight.beaveriot.rule.model.flow.route.base.OutputNode;
import org.springframework.core.Ordered;

/**
 * @author leon
 */
public interface RuleNodeInterceptor extends Ordered {

    default FromNode interceptFrom(String flowId, FromNode fromNode) {
        return fromNode;
    }

    default OutputNode interceptOutputNode(String flowId, OutputNode outputNode) {
        return outputNode;
    }

    default RouteNode interceptRouteNode(String flowId, RouteNode routeNode) {
        return routeNode;
    }

    @Override
    default int getOrder() {
        return 0;
    }

}
