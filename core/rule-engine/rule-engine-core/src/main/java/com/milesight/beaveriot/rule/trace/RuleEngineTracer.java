package com.milesight.beaveriot.rule.trace;

import com.milesight.beaveriot.rule.configuration.RuleProperties;
import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
import com.milesight.beaveriot.rule.enums.ExecutionStatus;
import com.milesight.beaveriot.rule.model.trace.FlowTraceInfo;
import com.milesight.beaveriot.rule.model.trace.NodeTraceInfo;
import com.milesight.beaveriot.rule.support.JsonHelper;
import com.milesight.beaveriot.rule.support.RuleFlowIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.NamedNode;
import org.apache.camel.NamedRoute;
import org.apache.camel.impl.engine.DefaultTracer;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.RouteDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author leon
 */
@Slf4j
public class RuleEngineTracer extends DefaultTracer {

    private ApplicationEventPublisher applicationEventPublisher;
    private RuleProperties ruleProperties;

    public RuleEngineTracer(ApplicationEventPublisher applicationEventPublisher, RuleProperties ruleProperties) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.ruleProperties = ruleProperties;
    }

    @Override
    public void traceBeforeRoute(NamedRoute route, Exchange exchange) {
        if (shouldTraceByLogging()) {
            super.traceBeforeRoute(route, exchange);
        }

        // only trace node with prefix
        if (!shouldTraceNodeByPrefix(route.getInput())) {
            return;
        }

        FlowTraceInfo flowTraceInfo = (FlowTraceInfo) exchange.getProperty(ExchangeHeaders.TRACE_RESPONSE);
        if (flowTraceInfo == null) {
            flowTraceInfo = new FlowTraceInfo();
            flowTraceInfo.setFlowId(exchange.getFromRouteId());
            exchange.setProperty(ExchangeHeaders.TRACE_RESPONSE, flowTraceInfo);
        }

        //add from node trace info
        if (route instanceof RouteDefinition routeDefinition) {
            FromDefinition input = routeDefinition.getInput();
            NodeTraceInfo nodeTraceInfo = createNodeTraceInfo(input.getId(), input.getLabel(), routeDefinition.getDescriptionText(), exchange);
            nodeTraceInfo.setOutput(getExchangeBody(exchange));
            flowTraceInfo.getTraceInfos().add(nodeTraceInfo);
        }
    }

    @Override
    public void traceBeforeNode(NamedNode node, Exchange exchange) {
        if (shouldTraceByLogging()) {
            super.traceBeforeNode(node, exchange);
        }
        FlowTraceInfo traceContext = (FlowTraceInfo) exchange.getProperty(ExchangeHeaders.TRACE_RESPONSE);
        if (traceContext != null && shouldTraceNodeByPrefix(node)) {
            try {
                NodeTraceInfo nodeTraceResponse = createNodeTraceInfo(node.getId(), node.getLabel(), node.getDescriptionText(), exchange);
                nodeTraceResponse.setInput(getExchangeBody(exchange));
                traceContext.getTraceInfos().add(nodeTraceResponse);
            } catch (Exception ex) {
                log.error("traceBeforeNode error", ex);
            }
        }
    }

    private NodeTraceInfo createNodeTraceInfo(String nodeId, String nodeLabel, String nodeName, Exchange exchange) {
        NodeTraceInfo nodeTraceResponse = new NodeTraceInfo();
        if (StringUtils.hasText(nodeLabel)) {
            nodeTraceResponse.setNodeLabel(nodeLabel.split("\\?")[0]);
        }
        nodeTraceResponse.setNodeId(RuleFlowIdGenerator.removeNamespacedId(exchange.getFromRouteId(), nodeId));
        nodeTraceResponse.setStartTime(System.currentTimeMillis());
        nodeTraceResponse.setMessageId(exchange.getIn().getMessageId());
        nodeTraceResponse.setNodeName(nodeName);
        return nodeTraceResponse;
    }

    @Override
    public void traceAfterNode(NamedNode node, Exchange exchange) {
        if (shouldTraceByLogging()) {
            super.traceAfterNode(node, exchange);
        }

        FlowTraceInfo traceContext = (FlowTraceInfo) exchange.getProperty(ExchangeHeaders.TRACE_RESPONSE);
        if (traceContext != null) {
            try {
                NodeTraceInfo traceInfo = traceContext.findTraceInfo(RuleFlowIdGenerator.removeNamespacedId(exchange.getFromRouteId(), node.getId()), exchange.getIn().getMessageId());
                if (traceInfo != null) {
                    traceInfo.setOutput(getExchangeBody(exchange));
                    traceInfo.setTimeCost(System.currentTimeMillis() - traceInfo.getStartTime());
                    traceInfo.setParentTraceId(exchange.getIn().getHeader(ExchangeHeaders.EXCHANGE_LATEST_TRACE_ID, String.class));
                    if (exchange.getException() != null) {
                        traceInfo.causeException(exchange.getException());
                    }
                }
            } catch (Exception ex) {
                log.error("traceBeforeNode error", ex);
            }
        }
    }

    private String getExchangeBody(Exchange exchange) {
        Object body = exchange.getIn().getBody();
        if (body == null) {
            return null;
        }
        try {
            if (body instanceof Exchange exchangeBody) {
                return JsonHelper.toJSON(exchangeBody.getIn().getBody());
            } else {
                return JsonHelper.toJSON(body);
            }
        } catch (Exception ex) {
            log.error("Convert exchange body failed", ex);
            return "Convert exchange body failed:" + ex.getMessage();
        }
    }

    @Override
    public void traceAfterRoute(NamedRoute route, Exchange exchange) {
        if (shouldTraceByLogging()) {
            super.traceAfterRoute(route, exchange);
        }

        FlowTraceInfo flowTraceResponse = (FlowTraceInfo) exchange.getProperty(ExchangeHeaders.TRACE_RESPONSE);
        if (shouldTraceByEvent() && flowTraceResponse != null && !flowTraceResponse.isEmpty()) {
            if (exchange.getException() != null) {
                flowTraceResponse.setStatus(ExecutionStatus.ERROR);
            }
            flowTraceResponse.setTimeCost(System.currentTimeMillis() - flowTraceResponse.getStartTime());
            log.debug("traceAfterRoute: {}", flowTraceResponse);
            // if trace for test, do not publish event
            Boolean traceForTest = exchange.getProperty(ExchangeHeaders.TRACE_FOR_TEST, false, boolean.class);
             if (Boolean.FALSE.equals(traceForTest)) {
                applicationEventPublisher.publishEvent(flowTraceResponse);
            }
        }
    }

    private boolean shouldTraceByEvent() {
        return ruleProperties.getTraceOutputMode() == RuleProperties.TraceOutputMode.ALL ||
                ruleProperties.getTraceOutputMode() == RuleProperties.TraceOutputMode.EVENT;
    }

    private boolean shouldTraceByLogging() {
        return ruleProperties.getTraceOutputMode() == RuleProperties.TraceOutputMode.ALL || ruleProperties.getTraceOutputMode() == RuleProperties.TraceOutputMode.LOGGING;
    }

    protected boolean shouldTraceNodeByPrefix(NamedNode node) {
        if (ObjectUtils.isEmpty(ruleProperties.getTraceNodePrefix())) {
            return true;
        }
        return node.getId().startsWith(ruleProperties.getTraceNodePrefix());
    }

}
