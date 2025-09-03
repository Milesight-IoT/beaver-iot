package com.milesight.beaveriot.blueprint.core.chart.node.data.value;

import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class LongValueNode extends AbstractValueNode<Long> {


  public LongValueNode(BlueprintNode blueprintNodeParent, String blueprintNodeName, Long value) {
    super(blueprintNodeParent, blueprintNodeName, value);
  }

}
