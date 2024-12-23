package com.milesight.beaveriot.rule.api;

import org.apache.camel.Exchange;

/**
 * @author leon
 */
public interface ProcessorNode<T> {

    void processor(T exchange);
}
