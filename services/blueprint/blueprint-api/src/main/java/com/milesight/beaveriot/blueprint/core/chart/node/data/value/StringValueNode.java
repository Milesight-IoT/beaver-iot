package com.milesight.beaveriot.blueprint.core.chart.node.data.value;

import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class StringValueNode extends AbstractValueNode<String> {

    public StringValueNode(BlueprintNode blueprintNodeParent, String blueprintNodeName, String value) {
        super(blueprintNodeParent, blueprintNodeName, value);
    }

}
