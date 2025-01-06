package com.milesight.beaveriot.rule.api;

/**
 * @author leon
 */
public interface ProcessorNode<T> {

    void processor(T exchange);
}
