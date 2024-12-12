package com.milesight.beaveriot.rule.model.flow.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * @author leon
 */
@Data
public class RuleNodeConfig implements RuleConfig {

    private String id;
    private String componentId;
    private String nodeName;
    private JsonNode parameters;

    public static RuleNodeConfig create(String id, String componentId, String nodeName, JsonNode parameters) {
        RuleNodeConfig ruleNodeConfig = new RuleNodeConfig();
        ruleNodeConfig.setId(id);
        ruleNodeConfig.setComponentId(componentId);
        ruleNodeConfig.setNodeName(nodeName);
        ruleNodeConfig.setParameters(parameters);
        return ruleNodeConfig;
    }

}
