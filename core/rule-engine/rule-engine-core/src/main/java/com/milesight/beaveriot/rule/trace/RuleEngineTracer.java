package com.milesight.beaveriot.rule.trace;

import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
import com.milesight.beaveriot.rule.enums.ExecutionStatus;
import com.milesight.beaveriot.rule.configuration.RuleProperties;
import com.milesight.beaveriot.rule.model.trace.FlowTraceResponse;
import com.milesight.beaveriot.rule.model.trace.NodeTraceResponse;
import com.milesight.beaveriot.rule.utils.JSONHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.NamedNode;
import org.apache.camel.NamedRoute;
import org.apache.camel.impl.engine.DefaultTracer;
import org.springframework.context.ApplicationEventPublisher;

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
            exchange.setProperty(ExchangeHeaders.TRACE_RESPONSE, new FlowTraceResponse());
        }
    }

    @Override
    public void traceBeforeNode(NamedNode node, Exchange exchange) {
        if (shouldTraceByLogging()) {
            super.traceBeforeNode(node, exchange);
        }

        FlowTraceResponse traceContext = (FlowTraceResponse) exchange.getProperty(ExchangeHeaders.TRACE_RESPONSE);
        if (traceContext != null) {
            try {
                NodeTraceResponse nodeTraceResponse = new NodeTraceResponse();
                nodeTraceResponse.setNodeName(node.getLabel());
                nodeTraceResponse.setNodeId(node.getId());
                nodeTraceResponse.setStartTime(System.currentTimeMillis());
                nodeTraceResponse.setInput(getInputBody(exchange));
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

        FlowTraceResponse traceContext = (FlowTraceResponse) exchange.getProperty(ExchangeHeaders.TRACE_RESPONSE);
        if (traceContext != null) {
            try {
                NodeTraceResponse traceInfo = traceContext.findTraceInfo(node.getId());
                if (exchange.getException() != null) {
                    traceInfo.causeException(exchange.getException());
                }
                if (traceInfo != null) {
                    traceInfo.setOutput(getInputBody(exchange));
                    traceInfo.setCost(System.currentTimeMillis() - traceInfo.getStartTime());
                }
            } catch (Exception ex) {
                log.error("traceBeforeNode error", ex);
            }
        }
    }

    private String getInputBody(Exchange exchange) {
        Object body = exchange.getIn().getBody();
        if (body == null) {
            return null;
        }
        if (body instanceof Exchange exchangeBody) {
            return JSONHelper.toJSON(exchangeBody.getIn().getBody());
        } else {
            return JSONHelper.toJSON(body);
        }
    }

    @Override
    public void traceAfterRoute(NamedRoute route, Exchange exchange) {
        if (shouldTraceByLogging()) {
            super.traceAfterRoute(route, exchange);
        }

        FlowTraceResponse flowTraceResponse = (FlowTraceResponse) exchange.getProperty(ExchangeHeaders.TRACE_RESPONSE);
        if (shouldTraceByEvent(exchange) && flowTraceResponse != null) {
            if (exchange.getException() != null) {
                flowTraceResponse.setStatus(ExecutionStatus.ERROR);
            }
            log.info("traceAfterRoute: {}", flowTraceResponse);
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

}
