package com.milesight.beaveriot.blueprint.core.chart.node.base;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public interface SequenceNode<T extends BlueprintNode> extends BlueprintNode {

    @JsonIgnore
    List<T> getTypedBlueprintNodeChildren();

}
