package com.milesight.beaveriot.rule.flow.yaml;

import com.milesight.beaveriot.rule.model.flow.yaml.FromNode;
import com.milesight.beaveriot.rule.model.flow.yaml.RouteNode;
import com.milesight.beaveriot.rule.model.flow.yaml.base.OutputNode;
import org.springframework.core.Ordered;

/**
 *
 * @author leon
 */
public interface RuleNodeInterceptor extends Ordered {

    default FromNode interceptFromNode(String flowId, FromNode fromNode) {
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
