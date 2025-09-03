package com.milesight.beaveriot.blueprint.core.chart.node.data.function;

import com.milesight.beaveriot.blueprint.core.chart.node.base.AbstractBlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
import com.milesight.beaveriot.blueprint.core.chart.node.enums.BlueprintNodeStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class AbstractFunctionNode extends AbstractBlueprintNode implements FunctionNode {

    protected AbstractFunctionNode(BlueprintNode blueprintNodeParent, String blueprintNodeName) {
        super(blueprintNodeParent, blueprintNodeName);
    }

    protected List<DataNode> parameters;

    protected DataNode result;

    @Override
    public List<BlueprintNode> getBlueprintNodeChildren() {
        return (List) parameters;
    }

    @Override
    public List<DataNode> getTypedBlueprintNodeChildren() {
        return parameters;
    }

    @Override
    public Object getValue() {
        if (!BlueprintNodeStatus.FINISHED.equals(blueprintNodeStatus)) {
            return null;
        }
        if (result != null) {
            return result.getValue();
        }
        return null;
    }
}
