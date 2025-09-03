package com.milesight.beaveriot.blueprint.core.chart.node.base;

import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@SuppressWarnings({"unchecked"})
@NoArgsConstructor
public abstract class AbstractMapNode<T extends BlueprintNode> extends AbstractBlueprintNode implements KeyValueNode<T> {

    protected AbstractMapNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    public List<T> getTypedChildren() {
        return (List<T>) getBlueprintNodeChildren();
    }

    public T getChild(String name) {
        Objects.requireNonNull(name);
        return (T) getBlueprintNodeChildren().stream()
                .filter(node -> name.equals(node.getBlueprintNodeName()))
                .findFirst()
                .orElse(null);
    }

    public void removeChild(String name) {
        Objects.requireNonNull(name);
        getBlueprintNodeChildren()
                .removeIf(node -> name.equals(node.getBlueprintNodeName()));
    }

}
