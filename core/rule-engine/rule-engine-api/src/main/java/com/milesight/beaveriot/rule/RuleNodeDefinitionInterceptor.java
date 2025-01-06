package com.milesight.beaveriot.rule;

import com.milesight.beaveriot.rule.model.flow.route.FromNodeDefinition;
import com.milesight.beaveriot.rule.model.flow.route.ToNodeDefinition;
import org.springframework.core.Ordered;

/**
 * @author leon
 */
public interface RuleNodeDefinitionInterceptor extends Ordered {

    default FromNodeDefinition interceptFromNodeDefinition(String flowId, FromNodeDefinition fromNode) {
        return fromNode;
    }

    default ToNodeDefinition interceptToNodeDefinition(String flowId, ToNodeDefinition toNodeDefinition) {
        return toNodeDefinition;
    }

    @Override
    default int getOrder() {
        return 0;
    }

}
