package com.milesight.beaveriot.blueprint.core.chart.node.data.container;

import com.milesight.beaveriot.blueprint.core.chart.node.base.AbstractArrayNode;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
public class ArrayDataNode extends AbstractArrayNode<DataNode> implements ContainerDataNode {

    public ArrayDataNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    @Override
    public List<Object> getValue() {
        return getTypedBlueprintNodeChildren().stream().map(DataNode::getValue).toList();
    }

}
