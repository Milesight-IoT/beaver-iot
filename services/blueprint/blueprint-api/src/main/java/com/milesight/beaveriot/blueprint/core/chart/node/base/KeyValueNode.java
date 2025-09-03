package com.milesight.beaveriot.blueprint.core.chart.node.base;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public interface KeyValueNode<T extends BlueprintNode> extends BlueprintNode {

    @JsonIgnore
    List<T> getTypedChildren();

    @JsonIgnore
    T getChild(String name);

    void removeChild(String name);

}
