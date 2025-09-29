package com.milesight.beaveriot.blueprint.core.chart.deploy.function;

import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FnGetNode;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FnGetExecutor extends AbstractFunctionExecutor<FnGetNode> {
    @Override
    public void execute(FnGetNode function, BlueprintDeployContext context) {
        var path = getParameter(function, 1, String.class);
        var source = function.getParameters().get(0);
        var node = BlueprintUtils.getChildByPath(source, path);
        if (node instanceof DataNode result) {
            setResult(function, result.getValue());
        }
    }

    @Override
    public Class<FnGetNode> getMatchedNodeType() {
        return FnGetNode.class;
    }

}
