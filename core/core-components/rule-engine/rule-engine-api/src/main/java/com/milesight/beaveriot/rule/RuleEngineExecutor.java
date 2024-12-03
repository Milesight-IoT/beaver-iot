package com.milesight.beaveriot.rule;

import org.apache.camel.Exchange;

import java.util.concurrent.CompletableFuture;

/**
 *
 * @author leon
 */
public interface RuleEngineExecutor {


    Object executeWithResponse(String endPointUri, Object payload);

    Exchange executeWithResponse(String endPointUri, Exchange exchange);

    void execute(String endpointUri, Object payload);

    void execute(String endPointUri, Exchange exchange);

    CompletableFuture<Exchange> asyncExecute(String endpointUri, Exchange exchange);

    CompletableFuture<Object> asyncExecute(String endpointUri, Object exchange);
}
