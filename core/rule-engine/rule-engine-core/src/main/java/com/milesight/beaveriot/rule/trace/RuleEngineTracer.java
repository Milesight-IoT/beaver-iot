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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.ObjectUtils;

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

        Object property = exchange.getProperty(ExchangeHeaders.TRACE_RESPONSE);
        if (property == null) {
            FlowTraceInfo flowTraceInfo = new FlowTraceInfo();
            flowTraceInfo.setFlowId(exchange.getFromRouteId());
            exchange.setProperty(ExchangeHeaders.TRACE_RESPONSE, flowTraceInfo);
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
                NodeTraceInfo nodeTraceResponse = new NodeTraceInfo();
                nodeTraceResponse.setNodeLabel(node.getLabel());
                nodeTraceResponse.setNodeId(RuleFlowIdGenerator.removeNamespacedId(exchange.getFromRouteId(), node.getId()));
                nodeTraceResponse.setStartTime(System.currentTimeMillis());
                nodeTraceResponse.setInput(getExchangeBody(exchange));
                nodeTraceResponse.setMessageId(exchange.getIn().getMessageId());
                traceContext.getTraceInfos().add(nodeTraceResponse);
            } catch (Exception ex) {
                log.error("traceBeforeNode error", ex);
            }
        }
    }

    @Override
    public void traceAfterNode(NamedNode node, Exchange exchange) {
        if (shouldTraceByLogging()) {
            super.traceAfterNode(node, exchange);
        }

        FlowTraceInfo traceContext = (FlowTraceInfo) exchange.getProperty(ExchangeHeaders.TRACE_RESPONSE);
        if (traceContext != null) {
            try {
                NodeTraceInfo traceInfo = traceContext.findTraceInfo(RuleFlowIdGenerator.removeNamespacedId(exchange.getFromRouteId(), node.getId()));
                if (traceInfo != null) {
                    traceInfo.setOutput(getExchangeBody(exchange));
                    traceInfo.setTimeCost(System.currentTimeMillis() - traceInfo.getStartTime());
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
        if (body instanceof Exchange exchangeBody) {
            return JsonHelper.toJSON(exchangeBody.getIn().getBody());
        } else {
            return JsonHelper.toJSON(body);
        }
    }

    @Override
    public void traceAfterRoute(NamedRoute route, Exchange exchange) {
        if (shouldTraceByLogging()) {
            super.traceAfterRoute(route, exchange);
        }

        FlowTraceInfo flowTraceResponse = (FlowTraceInfo) exchange.getProperty(ExchangeHeaders.TRACE_RESPONSE);
        if (shouldTraceByEvent(exchange) && flowTraceResponse != null) {
            if (exchange.getException() != null) {
                flowTraceResponse.setStatus(ExecutionStatus.ERROR);
            }
            log.debug("traceAfterRoute: {}", flowTraceResponse);
            applicationEventPublisher.publishEvent(flowTraceResponse);
        }
    }

    private boolean shouldTraceByEvent(Exchange exchange) {
        return ruleProperties.getTraceOutputMode() == RuleProperties.TraceOutputMode.ALL ||
                ruleProperties.getTraceOutputMode() == RuleProperties.TraceOutputMode.EVENT ||
                !exchange.getProperty(ExchangeHeaders.TRACE_FOR_TEST, false, Boolean.class);
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
