package com.milesight.beaveriot.device.blueprint;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.deploy.resource.ResourceManager;
import com.milesight.beaveriot.blueprint.core.chart.deploy.resource.ResourceMatcher;
import com.milesight.beaveriot.blueprint.core.chart.node.resource.DeviceCanvasResourceNode;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import com.milesight.beaveriot.blueprint.core.model.BindResource;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import com.milesight.beaveriot.canvas.facade.ICanvasFacade;
import com.milesight.beaveriot.canvas.model.request.CanvasUpdateRequest;
import com.milesight.beaveriot.device.service.DeviceCanvasService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DeviceCanvasResourceManager implements ResourceManager<DeviceCanvasResourceNode> {

    @Lazy
    @Autowired
    private DeviceCanvasService deviceCanvasService;

    @Lazy
    @Autowired
    private ICanvasFacade canvasFacade;

    @Override
    public Class<DeviceCanvasResourceNode> getMatchedNodeType() {
        return DeviceCanvasResourceNode.class;
    }

    @Override
    public List<BindResource> deploy(DeviceCanvasResourceNode canvasNode, BlueprintDeployContext context) {
        var accessor = canvasNode.getAccessor();
        var deviceId = accessor.getDeviceId();
        if (deviceId == null) {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_RESOURCE_DEPLOYMENT_FAILED, "Invalid property 'device_id'! Path: " + BlueprintUtils.getNodePath(canvasNode, context.getRoot()));
        }
        log.info("blueprint create canvas for device: {}", deviceId);

        var data = accessor.getData();
        if (data == null) {
            throw new ServiceException(BlueprintErrorCode.BLUEPRINT_RESOURCE_DEPLOYMENT_FAILED, "Invalid property 'data'! Path: " + BlueprintUtils.getNodePath(canvasNode, context.getRoot()));
        }

        var deviceCanvasResponse = deviceCanvasService.getOrCreateDeviceCanvas(Long.valueOf(deviceId));
        var canvasId = deviceCanvasResponse.getCanvasId();

        var canvasUpdateRequest = JsonUtils.withCamelCaseStrategy().cast(data, CanvasUpdateRequest.class);
        canvasFacade.updateCanvas(Long.valueOf(canvasId), canvasUpdateRequest);

        accessor.setId(canvasId);

        return List.of(new BindResource(DeviceCanvasResourceNode.RESOURCE_TYPE, canvasId, true));
    }

    @Override
    public boolean deleteResource(DeviceCanvasResourceNode resource, ResourceMatcher condition) {
        var accessor = resource.getAccessor();
        var id = accessor.getId();
        if (id == null) {
            log.warn("resource id not found: {}", BlueprintUtils.getNodePath(resource));
            return false;
        }

        if (resource.isManaged() && condition.isMatch(resource.getResourceType(), id)) {
            var canvasId = Long.valueOf(id);
            canvasFacade.deleteCanvasByIds(List.of(canvasId));
            return true;
        }
        return false;
    }

}
