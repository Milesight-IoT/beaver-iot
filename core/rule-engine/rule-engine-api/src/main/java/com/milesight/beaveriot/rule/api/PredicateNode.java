package com.milesight.beaveriot.rule.api;

/**
 * @author leon
 */
public interface PredicateNode<T> {

    boolean matches(T exchange);

}
