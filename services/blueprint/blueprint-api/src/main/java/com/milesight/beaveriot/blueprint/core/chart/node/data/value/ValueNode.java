package com.milesight.beaveriot.blueprint.core.chart.node.data.value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;

public interface ValueNode<T> extends DataNode {

    @JsonIgnore(false)
    @JsonProperty("@v")
    T getValue();

    @JsonProperty("@v")
    void setValue(T value);

}
