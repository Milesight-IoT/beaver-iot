package com.milesight.beaveriot.rule.observe;

import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import org.springframework.util.ObjectUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author leon
 */
public class RuleConfigOutputCache {

    private static Map<String, Map<String, RuleNodeConfig>> RULE_CONFIG_OUTPUT_CACHE = new ConcurrentHashMap<>();

    public static void cache(RuleFlowConfig ruleFlowConfig) {

        Map<String, RuleNodeConfig> ruleConfigs = new LinkedHashMap<>();
        ruleFlowConfig.getNodes().forEach(ruleNode -> {
            if (ruleNode instanceof RuleNodeConfig) {
                ruleConfigs.put(ruleNode.getId(), ruleNode);
            }
        });

        if (!ObjectUtils.isEmpty(ruleConfigs)) {
            RULE_CONFIG_OUTPUT_CACHE.put(ruleFlowConfig.getFlowId(), ruleConfigs);
        }
    }

    public static RuleNodeConfig get(String flowId, String nodeId) {
        //todo load from db
        return RULE_CONFIG_OUTPUT_CACHE.containsKey(flowId) ? RULE_CONFIG_OUTPUT_CACHE.get(flowId).get(nodeId) : null;
    }
}
