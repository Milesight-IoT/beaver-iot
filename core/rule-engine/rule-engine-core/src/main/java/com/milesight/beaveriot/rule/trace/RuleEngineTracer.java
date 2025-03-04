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

    private static final long TRACER_BODY_MAX_LENGTH = 100000;
    private static final String TRACER_BODY_EXCEED_MAX_WARNING = "Tracer body exceed maximum length " + TRACER_BODY_MAX_LENGTH;

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
                log.error("Before trace node log exceptions:", ex);
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
                log.error("After trace node log exceptions:", ex);
            }
        }
    }

    private String getExchangeBody(Exchange exchange) {
        Object body = exchange.getIn().getBody();
        if (body == null) {
            return null;
        }
        try {
            String bodyStr = (body instanceof Exchange exchangeBody) ? JsonHelper.toJSON(exchangeBody.getIn().getBody()) : JsonHelper.toJSON(body);
            return !ObjectUtils.isEmpty(bodyStr) && bodyStr.length() > TRACER_BODY_MAX_LENGTH ? TRACER_BODY_EXCEED_MAX_WARNING : bodyStr;
        } catch (Exception ex) {
            log.error("Convert exchange body failed on tracing", ex);
            return "Convert exchange body failed:" + ex.getMessage();
        }
    }

    @Override
    public void traceAfterRoute(NamedRoute route, Exchange exchange) {
        if (shouldTraceByLogging()) {
            super.traceAfterRoute(route, exchange);
        }

        FlowTraceInfo flowTraceResponse = (FlowTraceInfo) exchange.getProperty(ExchangeHeaders.TRACE_RESPONSE);
        if (shouldTraceAfterRoute(exchange, flowTraceResponse)) {
            if (exchange.getException() != null) {
                log.error("Execution workflow exception, flow ID: {}",flowTraceResponse.getFlowId(), exchange.getException());
                flowTraceResponse.setStatus(ExecutionStatus.ERROR);
            }
            flowTraceResponse.setTimeCost(System.currentTimeMillis() - flowTraceResponse.getStartTime());
            log.debug("After trace route log exceptions:: {}", flowTraceResponse);
            // if trace for test, do not publish event
            Boolean traceForTest = exchange.getProperty(ExchangeHeaders.TRACE_FOR_TEST, false, boolean.class);
             if (Boolean.FALSE.equals(traceForTest)) {
                applicationEventPublisher.publishEvent(flowTraceResponse);
            }
        }
    }

    private boolean shouldTraceAfterRoute(Exchange exchange, FlowTraceInfo flowTraceResponse) {
        if (ruleProperties.getTraceOutputMode() == RuleProperties.TraceOutputMode.LOGGING) {
            return false;
        }
        if (flowTraceResponse == null || flowTraceResponse.isEmpty()) {
            return false;
        }

        Boolean hasCollected = exchange.getProperty(ExchangeHeaders.TRACE_HAS_COLLECTED, Boolean.class);
        if (hasCollected == null || !hasCollected) {
            exchange.setProperty(ExchangeHeaders.TRACE_HAS_COLLECTED, true);
            return true;
        } else {
            return false;
        }
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
