package com.milesight.beaveriot.blueprint.core.chart.deploy.function;

import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FnConvertNode;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FnConvertExecutor extends AbstractFunctionExecutor<FnConvertNode> {
    @Override
    public void execute(FnConvertNode function, BlueprintDeployContext context) {
        var type = getParameter(function, 1, String.class);
        var value = getParameter(function, 0, Object.class, false);
        setResult(function, BlueprintUtils.convertValue(value, type));
    }

    @Override
    public Class<FnConvertNode> getMatchedNodeType() {
        return FnConvertNode.class;
    }

}
