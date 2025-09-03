package com.milesight.beaveriot.blueprint.core.chart.node.data.value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;

public interface ValueNode<T> extends DataNode {

    @JsonIgnore(false)
    T getValue();

    void setValue(T value);

}
