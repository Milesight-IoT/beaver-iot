package com.milesight.beaveriot.blueprint.core.chart.node.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked"})
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public abstract class AbstractMapNode<T extends BlueprintNode> extends AbstractBlueprintNode implements KeyValueNode<T> {

    @JsonIgnore
    private Map<String, BlueprintNode> nameToChild;

    protected AbstractMapNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    public List<T> getTypedChildren() {
        return (List<T>) getBlueprintNodeChildren();
    }

    @Override
    public void addChildNode(BlueprintNode node) {
        var name = node.getBlueprintNodeName();
        ensureNameToChildMappingInit();
        if (nameToChild.containsKey(name)) {
            removeChild(name);
        }
        super.addChildNode(node);
        nameToChild.put(name, node);
    }

    public T getChild(String name) {
        Objects.requireNonNull(name);
        ensureNameToChildMappingInit();
        return (T) nameToChild.get(name);
    }

    public void removeChild(String name) {
        Objects.requireNonNull(name);
        ensureNameToChildMappingInit();
        nameToChild.remove(name);
        getBlueprintNodeChildren()
                .removeIf(node -> name.equals(node.getBlueprintNodeName()));
    }

    @Override
    public void setBlueprintNodeChildren(List<BlueprintNode> blueprintNodeChildren) {
        super.setBlueprintNodeChildren(blueprintNodeChildren);
        initNameToChildMapping(blueprintNodeChildren);
    }

    private void initNameToChildMapping(List<BlueprintNode> blueprintNodeChildren) {
        nameToChild = blueprintNodeChildren.stream()
                .collect(Collectors.toMap(BlueprintNode::getBlueprintNodeName, Function.identity(), (a, b) -> b));
    }

    private void ensureNameToChildMappingInit() {
        if (nameToChild != null) {
            return;
        }
        initNameToChildMapping(getBlueprintNodeChildren());
    }
}
