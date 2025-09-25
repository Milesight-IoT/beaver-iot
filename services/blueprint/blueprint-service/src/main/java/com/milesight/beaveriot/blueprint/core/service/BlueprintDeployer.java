package com.milesight.beaveriot.blueprint.core.service;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.deploy.NodeDependencyDiscoverer;
import com.milesight.beaveriot.blueprint.core.chart.deploy.function.FunctionExecutor;
import com.milesight.beaveriot.blueprint.core.chart.deploy.resource.ResourceManager;
import com.milesight.beaveriot.blueprint.core.chart.deploy.resource.ResourceMatcher;
import com.milesight.beaveriot.blueprint.core.chart.node.base.BlueprintNode;
import com.milesight.beaveriot.blueprint.core.chart.node.data.function.FunctionNode;
import com.milesight.beaveriot.blueprint.core.chart.node.enums.BlueprintNodeStatus;
import com.milesight.beaveriot.blueprint.core.chart.node.resource.AbstractResourceNode;
import com.milesight.beaveriot.blueprint.core.chart.node.resource.ResourceNode;
import com.milesight.beaveriot.blueprint.core.chart.node.template.TemplateNode;
import com.milesight.beaveriot.blueprint.core.constant.BlueprintConstants;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import com.milesight.beaveriot.blueprint.core.model.BindResource;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BlueprintDeployer {

    @SuppressWarnings("rawtypes")
    private final Map<Class<?>, ResourceManager> typeToResourceManager;

    @SuppressWarnings("rawtypes")
    private final Map<Class<?>, FunctionExecutor> typeToFunctionExecutor;

    @SuppressWarnings("rawtypes")
    private final Map<Class<?>, NodeDependencyDiscoverer> typeToDependencyDiscoverer;

    public BlueprintDeployer(List<ResourceManager<? extends AbstractResourceNode>> resourceManagers, List<FunctionExecutor<? extends FunctionNode>> functionExecutors, List<NodeDependencyDiscoverer<? extends BlueprintNode>> dependencyResolvers) {
        typeToResourceManager = resourceManagers.stream()
                .collect(Collectors.toConcurrentMap(ResourceManager::getMatchedNodeType, Function.identity(), (a, b) -> a));
        typeToFunctionExecutor = functionExecutors.stream()
                .collect(Collectors.toConcurrentMap(FunctionExecutor::getMatchedNodeType, Function.identity(), (a, b) -> a));
        typeToDependencyDiscoverer = dependencyResolvers.stream()
                .collect(Collectors.toConcurrentMap(NodeDependencyDiscoverer::getMatchedNodeType, Function.identity(), (a, b) -> a));
    }

    public List<BindResource> deploy(TemplateNode blueprintRoot, Map<String, Object> templateContext) {
        var bindResources = new ArrayList<BindResource>();
        var variables = JsonUtils.toJsonNode(templateContext.getOrDefault(BlueprintConstants.VARIABLES_KEY, Collections.emptyMap()));
        var context = new BlueprintDeployContext(blueprintRoot, variables, templateContext);

        var stack = new ArrayDeque<BlueprintNode>();
        stack.push(blueprintRoot);

        while (!stack.isEmpty()) {
            var node = stack.pop();

            try {
                switch (node.getBlueprintNodeStatus()) {
                    case NOT_READY -> discoverDependencies(node, context, stack);
                    case PENDING -> deployNode(node, context, bindResources);
                    default -> {
                        // do nothing
                    }
                }
            } catch (Exception e) {
                var path = BlueprintUtils.getNodePath(node, context.getRoot());
                if (e instanceof ServiceException serviceException) {
                    serviceException.setDetailMessage("Occurred at: '" + path + "', Cause: " + serviceException.getDetailMessage());
                    throw serviceException;
                } else {
                    var errorCode = node instanceof FunctionNode ? BlueprintErrorCode.BLUEPRINT_FUNCTION_EXECUTION_FAILED : BlueprintErrorCode.BLUEPRINT_RESOURCE_DEPLOYMENT_FAILED;
                    throw new ServiceException(errorCode, "Occurred at: '" + path + "', Cause: " + e.getMessage(), e);
                }
            }
        }

        return mergeBindResources(bindResources);
    }

    public void delete(TemplateNode blueprintRoot, ResourceMatcher condition) {
        var stack = new ArrayDeque<BlueprintNode>();
        stack.push(blueprintRoot);
        while (!stack.isEmpty()) {
            var node = stack.pop();
            if (BlueprintNodeStatus.FINISHED.equals(node.getBlueprintNodeStatus())) {
                deleteNode(node, condition);
                node.getBlueprintNodeChildren().forEach(stack::push);
            }
        }
    }

    @NonNull
    private static List<BindResource> mergeBindResources(List<BindResource> bindResources) {
        var keyToBindResource = new HashMap<String, BindResource>();
        bindResources.forEach(bindResource -> keyToBindResource.compute(bindResource.getKey(),
                (k, exists) -> (exists == null || bindResource.managed()) ? bindResource : exists));
        return keyToBindResource.values().stream().toList();
    }

    @SuppressWarnings("unchecked")
    private void deployNode(BlueprintNode node, BlueprintDeployContext context, List<BindResource> bindResources) {
        if (node instanceof FunctionNode functionNode) {
            var executor = typeToFunctionExecutor.get(functionNode.getClass());
            if (executor == null) {
                throw new ServiceException(BlueprintErrorCode.BLUEPRINT_FUNCTION_EXECUTOR_NOT_FOUND);
            }
            executor.execute(functionNode, context);
        }

        if (node instanceof ResourceNode resourceNode) {
            var manager = typeToResourceManager.get(resourceNode.getClass());
            if (manager == null) {
                throw new ServiceException(BlueprintErrorCode.BLUEPRINT_RESOURCE_MANAGER_NOT_FOUND);
            }
            bindResources.addAll(manager.deploy(resourceNode, context));
        }

        node.setBlueprintNodeStatus(BlueprintNodeStatus.FINISHED);
    }

    @SuppressWarnings("unchecked")
    private void discoverDependencies(BlueprintNode node, BlueprintDeployContext context, Deque<BlueprintNode> stack) {
        var dependencies = new ArrayList<BlueprintNode>();
        var dependencyDiscoverer = typeToDependencyDiscoverer.get(node.getClass());
        if (dependencyDiscoverer != null) {
            dependencies.addAll(dependencyDiscoverer.discoverDependencies(node, context));
        } else {
            dependencies.addAll(node.getBlueprintNodeChildren());
        }

        node.setBlueprintNodeStatus(BlueprintNodeStatus.PENDING);
        stack.push(node);

        dependencies.stream()
                .filter(dependency -> !BlueprintNodeStatus.FINISHED.equals(dependency.getBlueprintNodeStatus()))
                .forEach(dependency -> {
                    switch (dependency.getBlueprintNodeStatus()) {
                        case PENDING ->
                                throw new ServiceException(BlueprintErrorCode.BLUEPRINT_CIRCULAR_DEPENDENCY_DETECTED);
                        case NOT_READY -> stack.push(dependency);
                        default -> {
                            // do nothing
                        }
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private void deleteNode(BlueprintNode blueprintNode, ResourceMatcher condition) {
        if (blueprintNode instanceof ResourceNode resourceNode) {
            var resourceManager = typeToResourceManager.get(resourceNode.getClass());
            if (resourceManager == null) {
                log.error("Resource manager not found! Node: {}", BlueprintUtils.getNodePath(resourceNode));
                throw new ServiceException(BlueprintErrorCode.BLUEPRINT_RESOURCE_MANAGER_NOT_FOUND);
            }

            var deleted = resourceManager.deleteResource(resourceNode, condition);
            if (deleted) {
                blueprintNode.setBlueprintNodeStatus(BlueprintNodeStatus.DELETED);
            }
        }
        blueprintNode.setBlueprintNodeStatus(BlueprintNodeStatus.FINISHED);
    }

}
