package com.milesight.beaveriot.rule.api;

/**
 * @author leon
 */
public interface PredicateNode<T> extends RuleConfigurationAware{

    boolean matches(T exchange) ;

}
