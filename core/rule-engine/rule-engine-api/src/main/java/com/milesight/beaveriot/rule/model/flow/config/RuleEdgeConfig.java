package com.milesight.beaveriot.rule.model.flow.config;

import lombok.Data;

/**
 * @author leon
 */
@Data
public class RuleEdgeConfig {

    private String id;

    private String source;

    private String target;

    private String sourceHandle;

    public static RuleEdgeConfig create(String source, String target, String sourceHandle) {
        RuleEdgeConfig ruleEdgeConfig = new RuleEdgeConfig();
        ruleEdgeConfig.setSource(source);
        ruleEdgeConfig.setTarget(target);
        ruleEdgeConfig.setSourceHandle(sourceHandle);
        return ruleEdgeConfig;
    }

}
