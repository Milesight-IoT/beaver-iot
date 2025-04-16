package com.milesight.beaveriot.rule;

import com.milesight.beaveriot.rule.model.flow.route.AbstractNodeDefinition;
import com.milesight.beaveriot.rule.model.flow.route.ChoiceNodeDefinition;
import com.milesight.beaveriot.rule.model.flow.route.FromNodeDefinition;
import com.milesight.beaveriot.rule.model.flow.route.ToNodeDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.springframework.core.Ordered;

/**
 * @author leon
 */
public interface RuleNodeDefinitionInterceptor extends Ordered {

    /**
     * FromNodeDefinition interceptor, supports modifying the starting node process parameters before building the Camel route.
     * @param flowId
     * @param fromNode
     * @return
     */
    default FromNodeDefinition interceptFromNodeDefinition(String flowId, FromNodeDefinition fromNode) {
        return fromNode;
    }

    /**
     *  ToNodeDefinition interceptor, supports modifying process parameters before building Camel route
     * @param flowId
     * @param toNodeDefinition
     * @return
     */
    default ToNodeDefinition interceptToNodeDefinition(String flowId, ToNodeDefinition toNodeDefinition) {
        return toNodeDefinition;
    }

    default ChoiceNodeDefinition interceptChoiceNodeDefinition(String flowId, ChoiceNodeDefinition choiceNodeDefinition) {
        return choiceNodeDefinition;
    }

    /**
     * ProcessorDefinition pre-processing, supports returning custom ProcessorDefinition
     * @param flowId
     * @param nodeDefinition
     * @return
     */
    default ProcessorDefinition postProcessNodeDefinition(String flowId, AbstractNodeDefinition nodeDefinition) {
        return null;
    }

    @Override
    default int getOrder() {
        return 0;
    }
}
