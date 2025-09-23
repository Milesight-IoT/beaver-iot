package com.milesight.beaveriot.rule.manager.blueprint;

import com.google.common.primitives.Longs;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.deploy.resource.ResourceManager;
import com.milesight.beaveriot.blueprint.core.chart.deploy.resource.ResourceMatcher;
import com.milesight.beaveriot.blueprint.core.chart.node.resource.WorkflowResourceNode;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import com.milesight.beaveriot.blueprint.core.model.BindResource;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import com.milesight.beaveriot.rule.manager.model.WorkflowCreateContext;
import com.milesight.beaveriot.rule.manager.model.request.SaveWorkflowRequest;
import com.milesight.beaveriot.rule.manager.service.WorkflowService;
import com.milesight.beaveriot.rule.support.JsonHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class WorkflowResourceManager implements ResourceManager<WorkflowResourceNode> {

    @Lazy
    @Autowired
    private WorkflowService workflowService;

    @Override
    public Class<WorkflowResourceNode> getMatchedNodeType() {
        return WorkflowResourceNode.class;
    }

    @Override
    public List<BindResource> deploy(WorkflowResourceNode workflowNode, BlueprintDeployContext context) {
        var accessor = workflowNode.getAccessor();

        var name = accessor.getName();
        if (name == null) {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_RESOURCE_DEPLOYMENT_FAILED, "Invalid property 'name'! Path: " + BlueprintUtils.getNodePath(workflowNode, context.getRoot()));
        }

        var deviceId = accessor.getDeviceId();
        if (deviceId == null) {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_RESOURCE_DEPLOYMENT_FAILED, "Invalid property 'device_id'! Path: " + BlueprintUtils.getNodePath(workflowNode, context.getRoot()));
        }

        var data = accessor.getData();
        if (data == null) {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_RESOURCE_DEPLOYMENT_FAILED, "Invalid property 'data'! Path: " + BlueprintUtils.getNodePath(workflowNode, context.getRoot()));
        }

        var remark = accessor.getRemark();
        var enabled = accessor.isEnabled();
        var request = new SaveWorkflowRequest();
        request.setName(name);
        request.setRemark(remark);
        request.setEnabled(enabled);
        request.setDesignData(JsonHelper.toJSON(data));

        log.info("create workflow: {}", name);

        var response = workflowService.createWorkflow(request, new WorkflowCreateContext(deviceId));
        var flowId = response.getFlowId();
        accessor.setId(flowId);

        return List.of(new BindResource(WorkflowResourceNode.RESOURCE_TYPE, flowId, true));
    }

    @Override
    public boolean deleteResource(WorkflowResourceNode resource, ResourceMatcher condition) {
        var accessor = resource.getAccessor();
        var id = accessor.getId();
        if (id == null) {
            log.warn("resource id not found: {}", BlueprintUtils.getNodePath(resource));
            return false;
        }

        if (resource.isManaged() && condition.isMatch(resource.getResourceType(), id)) {
            var flowId = Longs.tryParse(id);
            if (flowId != null) {
                workflowService.batchDelete(List.of(flowId));
                return true;
            }
        }
        return false;
    }
}
