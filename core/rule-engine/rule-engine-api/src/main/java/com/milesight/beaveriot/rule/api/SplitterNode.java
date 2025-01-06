package com.milesight.beaveriot.rule.api;

import java.util.List;

/**
 * @author leon
 */
public interface SplitterNode<T> {

    List<T> split(T payload);

}
