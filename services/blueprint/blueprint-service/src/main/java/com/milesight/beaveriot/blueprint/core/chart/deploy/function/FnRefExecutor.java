package com.milesight.beaveriot.blueprint.core.chart.deploy.function;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.deploy.NodeDependencyDiscoverer;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.DataNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FnRefNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FunctionNode;
import com.milesight.beaveriot.blueprint.core.chart.node.resource.ResourceNode;
import com.milesight.beaveriot.blueprint.core.chart.node.template.TemplateNode;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class FnRefExecutor extends AbstractFunctionExecutor<FnRefNode> implements NodeDependencyDiscoverer<FnRefNode> {

    public static final String PARAMETERS_PATH_PREFIX = TemplateNode.Fields.parameters + ".";

    @Override
    public void execute(FnRefNode function, BlueprintDeployContext context) {
        var path = getParameter(function, 0, String.class);
        var currentTemplate = BlueprintUtils.getCurrentTemplate(function);
        BlueprintNode searchFrom = currentTemplate;

        String type = null;
        if (StringUtils.uncapitalize(path).startsWith(PARAMETERS_PATH_PREFIX)) {
            if (currentTemplate.getBlueprintNodeParent() == null) {
                // index template can get parameter values from variables
                var variable = BlueprintUtils.getChildByPath(context.getVariables(), path);
                if (variable != null) {
                    setResult(function, variable);
                    return;
                }
            }
            searchFrom = currentTemplate.getParameterValues();
            path = path.substring(PARAMETERS_PATH_PREFIX.length());

            var jsonSchemaTypePath = getParameterTypePath(path);
            var jsonSchemaNode = BlueprintUtils.getChildByPath(currentTemplate.getParameters(), jsonSchemaTypePath);
            if (jsonSchemaNode instanceof DataNode jsonSchemaType) {
                var value = jsonSchemaType.getValue();
                if (value != null) {
                    type = value.toString();
                }
            }
        }

        var target = BlueprintUtils.getChildByPath(searchFrom, path);
        if (target == null) {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_FUNCTION_EXECUTION_FAILED, "Ref target not found! Target: '"+ path +"'");
        }

        if (target instanceof DataNode data) {
            var value = data.getValue();
            if (type != null) {
                try {
                    // try to convert parameter value if type is specified
                    value = BlueprintUtils.convertValue(value, type);
                } catch (IllegalArgumentException e) {
                    log.warn("Convert value failed.", e);
                }
            }
            setResult(function, BlueprintUtils.convertToDataNode(data.getBlueprintNodeName(), data.getBlueprintNodeParent(), value));
        } else {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_FUNCTION_EXECUTION_FAILED, "Ref target is not valid! Should be a parameter or property of resource! Target: '"+ path +"'");
        }
    }

    @Override
    public Class<FnRefNode> getMatchedNodeType() {
        return FnRefNode.class;
    }

    @Override
    public List<BlueprintNode> discoverDependencies(FnRefNode function, BlueprintDeployContext context) {
        var result = new ArrayList<BlueprintNode>();
        var path = getParameter(function, 0, String.class);
        var currentTemplate = BlueprintUtils.getCurrentTemplate(function);
        BlueprintNode searchFrom = currentTemplate;

        BlueprintNode parameterTypeNode = null;
        if (StringUtils.uncapitalize(path).startsWith(PARAMETERS_PATH_PREFIX)) {
            var parameterSubPath = path.substring(PARAMETERS_PATH_PREFIX.length());
            if (currentTemplate != context.getRoot()) {
                searchFrom = currentTemplate.getParameterValues();
                path = parameterSubPath;
            }

            if (searchFrom != null) {
                var parameterTypePath = getParameterTypePath(parameterSubPath);
                parameterTypeNode = getDependencyByPath(currentTemplate.getParameters(), parameterTypePath);
            }
        }

        if (searchFrom == null) {
            return Collections.emptyList();
        }

        var dependency = getDependencyByPath(searchFrom, path);
        if (dependency != null) {
            result.add(dependency);

            if (parameterTypeNode != null) {
                result.add(parameterTypeNode);
            }
        }

        return result;
    }

    @Nullable
    private static BlueprintNode getDependencyByPath(BlueprintNode searchFrom, String path) {
        var target = BlueprintUtils.getChildByPath(searchFrom, path);

        if (target == null) {
            var parent = BlueprintUtils.getChildByLongestMatchedPath(searchFrom, path);
            if (parent instanceof FunctionNode || parent instanceof ResourceNode) {
                return parent;
            }
        } else if (target instanceof DataNode) {
            return target;
        }

        return null;
    }

    private static String getParameterTypePath(String path) {
        var tokens = path.replace("[", ".[").split("\\.");
        if (tokens.length == 0) {
            return "";
        }

        var result = new StringBuilder();
        result.append(tokens[0]);
        for (var i = 1; i < tokens.length; i++) {
            var token = tokens[i];
            if (token.isEmpty()) {
                continue;
            }

            if (token.startsWith("[")) {
                result.append(".items");
            } else {
                result.append(".properties.");
                result.append(token);
            }
        }
        result.append(".type");

        return result.toString();
    }

}
