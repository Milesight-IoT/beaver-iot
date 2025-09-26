package com.milesight.beaveriot.blueprint.core.chart.node.base;

import com.milesight.beaveriot.blueprint.core.chart.node.enums.BlueprintNodeStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractBlueprintNode implements BlueprintNode {

    protected String blueprintNodeName;

    @ToString.Exclude
    protected BlueprintNode blueprintNodeParent;

    protected List<BlueprintNode> blueprintNodeChildren = new ArrayList<>();

    protected BlueprintNodeStatus blueprintNodeStatus = BlueprintNodeStatus.NOT_READY;

    protected AbstractBlueprintNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        this.blueprintNodeParent = blueprintNodeParent;
        this.blueprintNodeName = blueprintNodeName;
    }

    @Override
    public void addChildNode(BlueprintNode node) {
        blueprintNodeChildren.add(node);
    }

}
