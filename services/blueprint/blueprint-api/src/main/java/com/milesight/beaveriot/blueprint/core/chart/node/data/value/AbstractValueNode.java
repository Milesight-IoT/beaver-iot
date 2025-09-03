package com.milesight.beaveriot.blueprint.core.chart.node.data.value;

import com.milesight.beaveriot.blueprint.core.chart.node.base.AbstractBlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.enums.BlueprintNodeStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public abstract class AbstractValueNode<V> extends AbstractBlueprintNode implements ValueNode<V> {

    protected V value;

    protected AbstractValueNode(BlueprintNode blueprintNodeParent, String blueprintNodeName, V value) {
        super(blueprintNodeParent, blueprintNodeName);
        this.value = value;
    }

    @Override
    public BlueprintNodeStatus getBlueprintNodeStatus() {
        return BlueprintNodeStatus.FINISHED;
    }
}
