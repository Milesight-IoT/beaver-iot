package com.milesight.beaveriot.rule.interceptor;

import com.milesight.beaveriot.rule.RuleNodeDefinitionInterceptor;
import com.milesight.beaveriot.rule.model.flow.route.FromNodeDefinition;

import java.util.Map;

/**
 * @author loong
 */
public class TriggerInterceptor implements RuleNodeDefinitionInterceptor {

    @Override
    public FromNodeDefinition interceptFromNodeDefinition(String flowId, FromNodeDefinition fromNode) {
        String uri = fromNode.getUri();
        if (uri.startsWith("direct")) {
            Map<String, Object> parameters = fromNode.getParameters();
            if (parameters != null) {
                parameters.remove("entityConfigs");
            }
        }
        return fromNode;
    }

}
