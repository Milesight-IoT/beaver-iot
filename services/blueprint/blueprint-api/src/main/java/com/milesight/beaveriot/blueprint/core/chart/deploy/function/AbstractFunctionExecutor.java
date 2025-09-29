package com.milesight.beaveriot.blueprint.core.chart.deploy.function;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FunctionNode;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public abstract class AbstractFunctionExecutor<F extends FunctionNode> implements FunctionExecutor<F> {

    public static final String RESULT_KEY = "result";

    public <T> T getParameter(FunctionNode function, int pos, Class<T> type) {
        return getParameter(function, pos, type, true);
    }

    public <T> T getParameter(FunctionNode function, int pos, Class<T> type, boolean required) {
        var value = Optional.ofNullable(getParameter(function, pos))
                .map(DataNode::getValue)
                .orElse(null);
        if (value == null) {
            if (required) {
                throw new ServiceException(BlueprintErrorCode.BLUEPRINT_FUNCTION_EXECUTION_FAILED, "Parameter at [" + pos + "] is required.");
            }
            return null;
        }

        try {
            var result = BlueprintUtils.convertValue(value, type);
            if (result != null) {
                return result;
            }
        } catch (IllegalArgumentException e) {
            log.warn("Convert value failed.", e);
        }

        throw new ServiceException(BlueprintErrorCode.BLUEPRINT_FUNCTION_EXECUTION_FAILED, "Invalid parameter type at [" + pos + "]. Expected type is '" + type.getSimpleName() + "'.");
    }

    public DataNode getParameter(FunctionNode function, int pos) {
        return getParameter(function, pos, true);
    }

    public DataNode getParameter(FunctionNode function, int pos, boolean required) {
        var parameters = function.getParameters();
        if (parameters.size() - 1 < pos) {
            if (required) {
                throw new ServiceException(BlueprintErrorCode.BLUEPRINT_FUNCTION_EXECUTION_FAILED, "Invalid parameter count.");
            } else {
                return null;
            }
        }
        return parameters.get(pos);
    }

    public void setResult(FunctionNode function, Object result) {
        if (result == null) {
            function.setResult(null);
            return;
        }

        if (result instanceof DataNode dataNode) {
            dataNode.setBlueprintNodeName(RESULT_KEY);
            dataNode.setBlueprintNodeParent(function);
            function.setResult(dataNode);
        } else {
            function.setResult(BlueprintUtils.convertToDataNode(RESULT_KEY, function, result));
        }
    }

}
