package com.milesight.beaveriot.rule.model.flow.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * @author leon
 */
@Data
public class RuleNodeConfig implements RuleConfig {

    private String id;
    private String componentName;
    private RuleNodeDataConfig data;

    public static RuleNodeConfig create(String id, String componentName, String nodeName, JsonNode parameters) {
        RuleNodeConfig ruleNodeConfig = new RuleNodeConfig();
        ruleNodeConfig.setId(id);
        ruleNodeConfig.setComponentName(componentName);
        RuleNodeDataConfig ruleNodeDataConfig = new RuleNodeDataConfig();
        ruleNodeDataConfig.setNodeName(nodeName);
        ruleNodeDataConfig.setParameters(parameters);
        ruleNodeConfig.setData(ruleNodeDataConfig);
        return ruleNodeConfig;
    }

    public JsonNode getParameters() {
        return data != null ? data.getParameters() : null;
    }

    @Override
    public String getName() {
        return data != null ? data.getNodeName() : null;
    }

    @Data
    public static class RuleNodeDataConfig {
        private String nodeName;
        private JsonNode parameters;
    }

}
