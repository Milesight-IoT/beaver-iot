package com.milesight.beaveriot.permission.service;

import com.milesight.beaveriot.device.dto.DeviceNameDTO;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.permission.dto.DevicePermissionDTO;
import com.milesight.beaveriot.user.dto.UserResourceDTO;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.facade.IUserFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author loong
 * @date 2024/11/28 17:11
 */
@Service
public class DevicePermissionService {

    @Autowired
    IUserFacade userFacade;
    @Autowired
    IDeviceFacade deviceFacade;

    public DevicePermissionDTO getDevicePermission(Long userId) {
        DevicePermissionDTO devicePermissionDTO = new DevicePermissionDTO();
        UserResourceDTO userResourceDTO = userFacade.getResource(userId, Arrays.asList(ResourceType.DEVICE, ResourceType.INTEGRATION));
        devicePermissionDTO.setHasAllPermission(userResourceDTO.isHasAllResource());
        devicePermissionDTO.setDeviceIds(new ArrayList<>());
        if (!userResourceDTO.isHasAllResource()) {
            List<String> deviceIds = new ArrayList<>();
            Map<ResourceType, List<String>> resource = userResourceDTO.getResource();
            if (resource != null && !resource.isEmpty()) {
                resource.forEach((resourceType, resourceIds) -> {
                    if (resourceType == ResourceType.DEVICE) {
                        deviceIds.addAll(resourceIds);
                    } else if (resourceType == ResourceType.INTEGRATION) {
                        List<DeviceNameDTO> integrationDevices = deviceFacade.getDeviceNameByIntegrations(resourceIds);
                        if (integrationDevices != null && !integrationDevices.isEmpty()) {
                            List<String> integrationDeviceIds = integrationDevices.stream().map(t -> String.valueOf(t.getId())).toList();
                            deviceIds.addAll(integrationDeviceIds);
                        }
                    }
                });
            }
            devicePermissionDTO.setDeviceIds(deviceIds);
        }
        return devicePermissionDTO;
    }

}
