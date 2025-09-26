package com.milesight.beaveriot.blueprint.core.chart.deploy.function;

import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FunctionNode;

public interface FunctionExecutor<F extends FunctionNode> {

    Class<F> getMatchedNodeType();

    void execute(F function, BlueprintDeployContext context);

}
