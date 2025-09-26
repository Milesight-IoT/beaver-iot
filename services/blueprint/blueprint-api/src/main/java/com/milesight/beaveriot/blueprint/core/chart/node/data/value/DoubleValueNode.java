package com.milesight.beaveriot.blueprint.core.chart.node.data.value;

import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class DoubleValueNode extends AbstractValueNode<Double> {

  public DoubleValueNode(BlueprintNode blueprintNodeParent, String blueprintNodeName, Double value) {
    super(blueprintNodeParent, blueprintNodeName, value);
  }

}
