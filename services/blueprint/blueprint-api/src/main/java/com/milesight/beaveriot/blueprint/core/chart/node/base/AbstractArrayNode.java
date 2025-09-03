package com.milesight.beaveriot.blueprint.core.chart.node.base;

import lombok.NoArgsConstructor;

import java.util.List;

@SuppressWarnings({"unchecked"})
@NoArgsConstructor
public abstract class AbstractArrayNode<T extends BlueprintNode> extends AbstractBlueprintNode implements SequenceNode<T> {

    protected AbstractArrayNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    @Override
    public List<T> getTypedBlueprintNodeChildren() {
        return (List<T>) getBlueprintNodeChildren();
    }

}
