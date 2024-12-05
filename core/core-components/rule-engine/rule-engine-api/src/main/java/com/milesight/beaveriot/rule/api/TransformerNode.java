package com.milesight.beaveriot.rule.api;

/**
 * @author leon
 */
public interface TransformerNode<S, T> {

    T transform(S exchange);
}
