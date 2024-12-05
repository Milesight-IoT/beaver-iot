package com.milesight.beaveriot.rule.flow;

import com.milesight.beaveriot.rule.RuleEngineExecutor;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;

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
    public Object executeWithResponse(String endPointUri, Object payload) {
        return producerTemplate.sendBody(endPointUri, ExchangePattern.InOut, payload);
    }

    @Override
    public Exchange executeWithResponse(String endPointUri, Exchange exchange) {
        return producerTemplate.send(endPointUri, exchange);
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
