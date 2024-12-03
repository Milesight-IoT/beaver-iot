package com.milesight.beaveriot.rule.api;

/**
 * @author leon
 */
public interface SourceNode<T> extends RuleConfigurationAware{

    void processor(T exchange) ;
}
