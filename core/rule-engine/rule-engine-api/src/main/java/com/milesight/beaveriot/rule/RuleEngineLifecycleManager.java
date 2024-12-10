package com.milesight.beaveriot.rule;

import com.milesight.beaveriot.rule.model.trace.FlowTraceResponse;
import com.milesight.beaveriot.rule.model.trace.NodeTraceRequest;
import com.milesight.beaveriot.rule.model.trace.NodeTraceResponse;
import org.apache.camel.Exchange;

/**
 * @author leon
 */
public interface RuleEngineLifecycleManager {


    void deployFlow(String flowId, String flowYaml) throws Exception;

    void startRoute(String flowId) throws Exception;

    void stopRoute(String flowId) throws Exception;

    void removeFlow(String flowId) throws Exception;

    FlowTraceResponse trackFlow(String routeId, Exchange exchange);         //todo

    NodeTraceResponse trackNode(NodeTraceRequest nodeTraceRequest, Exchange exchange);         //todo

}
