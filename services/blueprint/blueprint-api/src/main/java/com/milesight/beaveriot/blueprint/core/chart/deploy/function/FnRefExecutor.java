package com.milesight.beaveriot.blueprint.core.chart.deploy.function;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.deploy.NodeDependencyDiscoverer;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FnRefNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FunctionNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.value.StringValueNode;
import com.milesight.beaveriot.blueprint.core.chart.node.resource.ResourceNode;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class FnRefExecutor implements FunctionExecutor<FnRefNode>, NodeDependencyDiscoverer<FnRefNode> {

    public static final String PARAMETERS_PATH_PREFIX = "parameters.";
    public static final String RESULT_KEY = "result";

    @Override
    public void execute(FnRefNode function, BlueprintDeployContext context) {
        var path = getPath(function);
        if (!StringUtils.hasText(path)) {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_FUNCTION_EXECUTION_FAILED, "Parameter is required! Path: " + BlueprintUtils.getNodePath(function, context.getRoot()));
        }

        var currentTemplate = BlueprintUtils.getCurrentTemplate(function);
        BlueprintNode searchFrom = currentTemplate;
        if (path.startsWith(PARAMETERS_PATH_PREFIX)) {
            if (currentTemplate.getBlueprintNodeParent() == null) {
                // index template can get parameter values from variables
                var variable = BlueprintUtils.getChildByPath(context.getVariables(), path);
                if (variable != null) {
                    function.setResult(BlueprintUtils.convertToDataNode(RESULT_KEY, function, variable));
                    return;
                }
            }
            searchFrom = currentTemplate.getParameterValues();
            path = path.substring(PARAMETERS_PATH_PREFIX.length());
        }

        var target = BlueprintUtils.getChildByPath(searchFrom, path);
        if (target == null) {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_FUNCTION_EXECUTION_FAILED, "Ref target not found! Path: " + BlueprintUtils.getNodePath(function, context.getRoot()));
        }
        if (target instanceof DataNode data) {
            var copy = JsonUtils.copy(data);
            copy.setBlueprintNodeName(RESULT_KEY);
            copy.setBlueprintNodeParent(function);
            function.setResult(copy);
        } else {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_FUNCTION_EXECUTION_FAILED, "Ref target is not valid! Should be a parameter or property of resource! Path: " + BlueprintUtils.getNodePath(function, context.getRoot()));
        }
    }

    @Nullable
    private static String getPath(FnRefNode function) {
        if (CollectionUtils.isEmpty(function.getParameters())) {
            return null;
        }
        var parameter = function.getParameters().get(0);
        if (parameter instanceof StringValueNode stringValueNode) {
            return stringValueNode.getValue();
        }
        if (parameter instanceof FunctionNode functionNode) {
            return String.valueOf(functionNode.getValue());
        }
        return null;
    }

    @Override
    public Class<FnRefNode> getMatchedNodeType() {
        return FnRefNode.class;
    }

    @Override
    public List<BlueprintNode> discoverDependencies(FnRefNode function, BlueprintDeployContext context) {
        var path = getPath(function);
        if (!StringUtils.hasText(path)) {
            return Collections.emptyList();
        }

        var currentTemplate = BlueprintUtils.getCurrentTemplate(function);
        BlueprintNode searchFrom = currentTemplate;
        if (path.startsWith(PARAMETERS_PATH_PREFIX) && currentTemplate != context.getRoot()) {
            searchFrom = currentTemplate.getParameterValues();
            path = path.substring(PARAMETERS_PATH_PREFIX.length());
        }

        if (searchFrom == null) {
            return Collections.emptyList();
        }

        var target = BlueprintUtils.getChildByPath(searchFrom, path);
        if (target == null) {
            var parent = BlueprintUtils.getChildByLongestMatchedPath(searchFrom, path);
            if (parent instanceof FunctionNode || parent instanceof ResourceNode) {
                return List.of(parent);
            }
        } else if (target instanceof DataNode) {
            return List.of(target);
        }

        return Collections.emptyList();
    }

}
