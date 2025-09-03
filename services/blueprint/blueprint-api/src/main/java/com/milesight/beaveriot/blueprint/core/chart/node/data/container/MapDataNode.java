package com.milesight.beaveriot.blueprint.core.chart.node.data.container;

import com.milesight.beaveriot.blueprint.core.chart.node.base.AbstractMapNode;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
import com.milesight.beaveriot.blueprint.core.chart.node.enums.BlueprintNodeStatus;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor
public class MapDataNode extends AbstractMapNode<DataNode> implements ContainerDataNode {

    public MapDataNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    @Override
    public Map<String, Object> getValue() {
        if (!BlueprintNodeStatus.FINISHED.equals(blueprintNodeStatus)) {
            return null;
        }
        return getTypedChildren().stream()
                .collect(Collectors.toMap(BlueprintNode::getBlueprintNodeName, DataNode::getValue, (a, b) -> a));
    }

}
