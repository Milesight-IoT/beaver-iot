package com.milesight.beaveriot.blueprint.core.chart.node.template;

import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.container.MapDataNode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ParameterValues extends MapDataNode {

    public ParameterValues(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

}
