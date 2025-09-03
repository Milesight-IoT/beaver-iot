package com.milesight.beaveriot.blueprint.core.chart.node.resource;


import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;

public interface ResourceNode extends BlueprintNode {

    String getResourceType();

    boolean isManaged();

    void setManaged(boolean managed);

}
