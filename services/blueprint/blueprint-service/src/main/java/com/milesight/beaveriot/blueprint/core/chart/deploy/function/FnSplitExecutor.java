package com.milesight.beaveriot.blueprint.core.chart.deploy.function;

import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FnSplitNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
public class FnSplitExecutor extends AbstractFunctionExecutor<FnSplitNode> {
    @Override
    public void execute(FnSplitNode function, BlueprintDeployContext context) {
        var delimiter = getParameter(function, 0, String.class);
        var sourceString = getParameter(function, 1, String.class);
        setResult(function, Arrays.asList(sourceString.split(delimiter)));
    }

    @Override
    public Class<FnSplitNode> getMatchedNodeType() {
        return FnSplitNode.class;
    }

}
