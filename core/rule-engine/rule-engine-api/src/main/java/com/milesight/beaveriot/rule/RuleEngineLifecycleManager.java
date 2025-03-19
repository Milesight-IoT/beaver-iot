package com.milesight.beaveriot.rule;

import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.model.trace.FlowTraceInfo;
import com.milesight.beaveriot.rule.model.trace.NodeTraceInfo;
import org.apache.camel.Exchange;

/**
 * @author leon
 */
public interface RuleEngineLifecycleManager {

    void deployFlow(RuleFlowConfig ruleFlowConfig);

    void deployFlow(String flowId, String flowRouteYaml);

    void startRoute(String flowId);

    void stopRoute(String flowId);

    boolean removeFlow(String flowId);

    boolean removeFlowImmediately(String flowId);

    boolean validateFlow(RuleFlowConfig ruleFlowConfig);

    FlowTraceInfo trackFlow(RuleFlowConfig ruleFlowConfig, Exchange exchange);

    FlowTraceInfo trackFlow(RuleFlowConfig ruleFlowConfig, Object body);

    NodeTraceInfo trackNode(RuleNodeConfig nodeConfig, Exchange exchange);

    NodeTraceInfo trackNode(RuleNodeConfig nodeConfig, Object body);

}
