package com.milesight.beaveriot.blueprint.core.chart.deploy.function;

import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FnGetNode;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FnGetExecutor extends AbstractFunctionExecutor<FnGetNode> {
    @Override
    public void execute(FnGetNode function, BlueprintDeployContext context) {
        var source = getParameter(function, 0, Object.class);
        var path = getParameter(function, 1, String.class);
        var result = BlueprintUtils.getChildByPath(JsonUtils.toJsonNode(source), path);
        setResult(function, result);
    }

    @Override
    public Class<FnGetNode> getMatchedNodeType() {
        return FnGetNode.class;
    }

}
