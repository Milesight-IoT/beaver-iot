package com.milesight.beaveriot.blueprint.core.chart.node.data.container;

import com.milesight.beaveriot.blueprint.core.chart.node.base.AbstractMapNode;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
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
        return getTypedChildren().stream()
                .map(Pair::of)
                .filter(Pair::nonNull)
                .collect(Collectors.toMap(Pair::name, Pair::value, (a, b) -> a));
    }

    private record Pair(String name, Object value) {

        public static Pair of(DataNode blueprintNode) {
            return new Pair(blueprintNode.getBlueprintNodeName(), blueprintNode.getValue());
        }

        public boolean nonNull() {
            return value != null;
        }

    }

}
