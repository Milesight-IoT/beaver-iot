package com.milesight.beaveriot.blueprint.core.chart.node.resource;


import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;

public interface ResourceNode extends BlueprintNode {

    DataNode getId();

    String getResourceType();

    boolean isManaged();

    void setManaged(boolean managed);

}
