package com.milesight.beaveriot.rule;

import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.model.trace.FlowTraceResponse;
import com.milesight.beaveriot.rule.model.trace.NodeTraceResponse;
import org.apache.camel.Exchange;

/**
 * @author leon
 */
public interface RuleEngineLifecycleManager {

    String deployFlow(RuleFlowConfig ruleFlowConfig);

    void deployFlow(String flowId, String flowRouteYaml);

    void startRoute(String flowId);

    void stopRoute(String flowId);

    void removeFlow(String flowId);

    boolean validateFlow(RuleFlowConfig ruleFlowConfig);

    FlowTraceResponse trackFlow(RuleFlowConfig ruleFlowConfig, Exchange exchange);

    FlowTraceResponse trackFlow(RuleFlowConfig ruleFlowConfig, Object body);

    NodeTraceResponse trackNode(RuleNodeConfig nodeConfig, Exchange exchange);

    NodeTraceResponse trackNode(RuleNodeConfig nodeConfig, Object body);

}
