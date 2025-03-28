package com.milesight.beaveriot.rule.flow;

import com.milesight.beaveriot.rule.RuleEngineExecutor;
import org.apache.camel.*;
import org.apache.camel.support.ExchangeHelper;
import org.springframework.util.ObjectUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author leon
 */
public class CamelRuleEngineExecutor implements RuleEngineExecutor {

    private ProducerTemplate producerTemplate;

    public void initializeCamelContext(CamelContext camelContext) {
        this.producerTemplate = camelContext.createProducerTemplate();
    }

    @Override
    public void execute(String endpointUri, Object payload) {
        producerTemplate.sendBody(endpointUri, payload);
    }

    @Override
    public void execute(String endPointUri, Exchange exchange) {
        producerTemplate.send(endPointUri, exchange);
    }

    @Override
    public void execute(String endpointUri, Object payload, Map<String, Object> properties) {
        producerTemplate.send(endpointUri, createProcessor(payload, properties));
    }
    @Override
    public Object executeWithResponse(String endPointUri, Object payload) {
        return producerTemplate.sendBody(endPointUri, ExchangePattern.InOut, payload);
    }

    @Override
    public Object executeWithResponse(String endPointUri, Object payload, Map<String, Object> properties) {
        Exchange result = producerTemplate.send(endPointUri, ExchangePattern.InOut, createProcessor(payload, properties));
        return ExchangeHelper.extractResultBody(result, ExchangePattern.InOut);
    }

    @Override
    public Exchange executeWithResponse(String endPointUri, Exchange exchange) {
        return producerTemplate.send(endPointUri, exchange);
    }

    private Processor createProcessor(Object payload, Map<String,Object> properties) {
        return exchange -> {
            if (!ObjectUtils.isEmpty(properties)) {
                properties.entrySet().forEach(entry -> {
                    exchange.setProperty(entry.getKey(), entry.getValue());
                });
            }
            Message in = exchange.getIn();
            in.setBody(payload);
        };
    }

    @Override
    public CompletableFuture<Exchange> asyncExecute(String endpointUri, Exchange exchange) {
        return producerTemplate.asyncSend(endpointUri, exchange);
    }

    @Override
    public CompletableFuture<Object> asyncExecute(String endpointUri, Object exchange) {
        return producerTemplate.asyncSendBody(endpointUri, exchange);
    }
}
