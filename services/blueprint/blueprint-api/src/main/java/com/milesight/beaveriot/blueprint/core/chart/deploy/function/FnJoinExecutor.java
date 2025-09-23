package com.milesight.beaveriot.blueprint.core.chart.deploy.function;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FnJoinNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.value.StringValueNode;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class FnJoinExecutor implements FunctionExecutor<FnJoinNode> {
    @Override
    public void execute(FnJoinNode function, BlueprintDeployContext context) {
        var parameters = function.getParameters();
        if (parameters.size() != 2) {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_FUNCTION_EXECUTION_FAILED, "Invalid parameter count. Path: " + BlueprintUtils.getNodePath(function, context.getRoot()));
        }

        var parameter1 = parameters.get(0).getValue();
        if (!(parameter1 instanceof String delimiter)) {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_FUNCTION_EXECUTION_FAILED, "Invalid parameter type [0]. Should be a string. Path: " + BlueprintUtils.getNodePath(function, context.getRoot()));
        }

        var parameter2 = parameters.get(1).getValue();
        if (!(parameter2 instanceof List<?> elements)) {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_FUNCTION_EXECUTION_FAILED, "Invalid parameter type [1]. Should be a list. Path: " + BlueprintUtils.getNodePath(function, context.getRoot()));
        }

        var result = new StringBuilder();
        var lastElementIndex = elements.size() - 1;
        for (int i = 0; i < lastElementIndex; i++) {
            var element = elements.get(i);
            result.append(element).append(delimiter);
        }
        result.append(elements.get(lastElementIndex));
        function.setResult(new StringValueNode(function, "result", result.toString()));
    }

    @Override
    public Class<FnJoinNode> getMatchedNodeType() {
        return FnJoinNode.class;
    }

}
