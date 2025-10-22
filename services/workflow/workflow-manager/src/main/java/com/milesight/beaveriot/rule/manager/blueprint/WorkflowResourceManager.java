package com.milesight.beaveriot.rule.manager.blueprint;

import com.google.common.primitives.Longs;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.deploy.resource.ResourceManager;
import com.milesight.beaveriot.blueprint.core.chart.deploy.resource.ResourceMatcher;
import com.milesight.beaveriot.blueprint.core.chart.node.resource.WorkflowResourceNode;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import com.milesight.beaveriot.blueprint.core.model.BindResource;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import com.milesight.beaveriot.permission.enums.DataPermissionType;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import com.milesight.beaveriot.permission.helper.TemporaryPermission;
import com.milesight.beaveriot.rule.facade.IWorkflowFacade;
import com.milesight.beaveriot.rule.manager.model.WorkflowAdditionalData;
import com.milesight.beaveriot.rule.manager.model.WorkflowCreateContext;
import com.milesight.beaveriot.rule.manager.model.request.SaveWorkflowRequest;
import com.milesight.beaveriot.rule.manager.service.WorkflowService;
import com.milesight.beaveriot.rule.support.JsonHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class WorkflowResourceManager implements ResourceManager<WorkflowResourceNode> {

    @Lazy
    @Autowired
    private WorkflowService workflowService;

    @Lazy
    @Autowired
    private IWorkflowFacade workflowFacade;

    @Override
    public Class<WorkflowResourceNode> getMatchedNodeType() {
        return WorkflowResourceNode.class;
    }

    @Override
    public List<BindResource> deploy(WorkflowResourceNode workflowNode, BlueprintDeployContext context) {
        var accessor = workflowNode.getAccessor();
        var flowId = accessor.getId();
        var isManaged = flowId == null;
        if (!isManaged) {
            try {
                var flowIdNumber = Longs.tryParse(flowId);
                var existsWorkflow = TemporaryPermission.with(OperationPermissionCode.WORKFLOW_VIEW)
                        .supply(() -> workflowService.getWorkflowDesign(flowIdNumber, null));
                Optional.ofNullable(existsWorkflow.getAdditionalData())
                        .map(WorkflowAdditionalData::getDeviceId)
                        .map(Longs::tryParse)
                        .ifPresent(accessor::setDeviceId);
                accessor.setName(existsWorkflow.getName());
                accessor.setRemark(existsWorkflow.getRemark());
                accessor.setEnabled(existsWorkflow.getEnabled());
                accessor.setData(JsonHelper.fromJSON(existsWorkflow.getDesignData()));

            } catch (ServiceException e) {
                if (ErrorCode.DATA_NO_FOUND.getErrorCode().equals(e.getErrorCode())) {
                    throw new ServiceException(BlueprintErrorCode.BLUEPRINT_RESOURCE_DEPLOYMENT_FAILED, "Workflow '" + flowId + "' not found.");
                } else {
                    throw e;
                }
            }

        } else {
            var name = accessor.getName();
            if (name == null) {
                throw new ServiceException(BlueprintErrorCode.BLUEPRINT_RESOURCE_DEPLOYMENT_FAILED, "Invalid property: 'name'.");
            }

            var deviceId = accessor.getDeviceId();
            if (deviceId == null) {
                throw new ServiceException(BlueprintErrorCode.BLUEPRINT_RESOURCE_DEPLOYMENT_FAILED, "Invalid property: 'device_id'.");
            }

            var data = accessor.getData();
            if (data == null) {
                throw new ServiceException(BlueprintErrorCode.BLUEPRINT_RESOURCE_DEPLOYMENT_FAILED, "Invalid property: 'data'.");
            }

            var remark = accessor.getRemark();
            var enabled = accessor.isEnabled();
            var request = new SaveWorkflowRequest();
            request.setName(name);
            request.setRemark(remark);
            request.setEnabled(enabled);
            request.setDesignData(JsonHelper.toJSON(data));

            log.info("create workflow: {}", name);

            var response = TemporaryPermission.with(OperationPermissionCode.WORKFLOW_ADD)
                    .with(DataPermissionType.DEVICE, String.valueOf(deviceId))
                    .supply(() -> workflowService.createWorkflow(request, new WorkflowCreateContext(deviceId)));
            flowId = response.getFlowId();
            accessor.setId(flowId);
        }

        var trigger = workflowFacade.getTriggerEntityByWorkflow(Long.valueOf(flowId));
        if (trigger != null) {
            accessor.setTrigger(JsonUtils.toJsonNode(trigger));
        }

        workflowNode.setManaged(isManaged);
        return List.of(new BindResource(WorkflowResourceNode.RESOURCE_TYPE, flowId, isManaged));
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
                log.info("delete workflow: {}", flowId);
                TemporaryPermission.with(OperationPermissionCode.WORKFLOW_DELETE)
                                .run(() -> workflowService.batchDelete(List.of(flowId)));
                return true;
            }
        }
        return false;
    }
}
