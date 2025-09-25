package com.milesight.beaveriot.device.blueprint;

import com.google.common.primitives.Longs;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.blueprint.core.chart.deploy.BlueprintDeployContext;
import com.milesight.beaveriot.blueprint.core.chart.deploy.resource.ResourceManager;
import com.milesight.beaveriot.blueprint.core.chart.deploy.resource.ResourceMatcher;
import com.milesight.beaveriot.blueprint.core.chart.node.resource.DeviceCanvasResourceNode;
import com.milesight.beaveriot.blueprint.core.enums.BlueprintErrorCode;
import com.milesight.beaveriot.blueprint.core.model.BindResource;
import com.milesight.beaveriot.blueprint.core.utils.BlueprintUtils;
import com.milesight.beaveriot.canvas.enums.CanvasAttachType;
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
        var canvasId = accessor.getId();
        var isManaged = canvasId == null;
        if (!isManaged) {
            try {
                var existsCanvasData = canvasFacade.getCanvasData(Longs.tryParse(canvasId));

                if (CanvasAttachType.DEVICE.equals(existsCanvasData.getAttachType())) {
                    accessor.setDeviceId(Longs.tryParse(existsCanvasData.getAttachId()));
                } else {
                    throw new ServiceException(BlueprintErrorCode.BLUEPRINT_RESOURCE_DEPLOYMENT_FAILED, "Canvas '" + canvasId + "' is not a 'device-canvas'.");
                }

                existsCanvasData.setDevices(null);
                existsCanvasData.setEntities(null);
                accessor.setData(JsonUtils.withCamelCaseStrategy().toJsonNode(existsCanvasData));

            } catch (ServiceException e) {
                if (ErrorCode.DATA_NO_FOUND.getErrorCode().equals(e.getErrorCode())) {
                    throw new ServiceException(BlueprintErrorCode.BLUEPRINT_RESOURCE_DEPLOYMENT_FAILED, "Canvas '" + canvasId + "' not found.");
                } else {
                    throw e;
                }
            }

        } else {
            var deviceId = accessor.getDeviceId();
            if (deviceId == null) {
                throw new ServiceException(BlueprintErrorCode.BLUEPRINT_RESOURCE_DEPLOYMENT_FAILED, "Invalid property: 'device_id'.");
            }
            log.info("blueprint create canvas for device: {}", deviceId);

            var data = accessor.getData();
            if (data == null) {
                throw new ServiceException(BlueprintErrorCode.BLUEPRINT_RESOURCE_DEPLOYMENT_FAILED, "Invalid property: 'data'.");
            }

            var deviceCanvasResponse = deviceCanvasService.getOrCreateDeviceCanvas(deviceId);
            canvasId = deviceCanvasResponse.getCanvasId();

            var canvasUpdateRequest = JsonUtils.withCamelCaseStrategy().cast(data, CanvasUpdateRequest.class);
            canvasUpdateRequest.setName(deviceCanvasResponse.getName());
            canvasFacade.updateCanvas(Longs.tryParse(canvasId), canvasUpdateRequest);
            accessor.setId(canvasId);
        }

        canvasNode.setManaged(isManaged);
        return List.of(new BindResource(DeviceCanvasResourceNode.RESOURCE_TYPE, canvasId, isManaged));
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
            var canvasId = Longs.tryParse(id);
            if (canvasId != null) {
                log.info("delete canvas: {}", canvasId);
                canvasFacade.deleteCanvasByIds(List.of(canvasId));
                return true;
            }
        }
        return false;
    }

}
