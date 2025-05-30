package com.milesight.beaveriot.permission.service;

import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.permission.dto.PermissionDTO;
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
    private IUserFacade userFacade;
    @Autowired
    private IDeviceFacade deviceFacade;

    public PermissionDTO getDevicePermission(Long userId) {
        PermissionDTO permissionDTO = new PermissionDTO();
        UserResourceDTO userResourceDTO = userFacade.getResource(userId, Arrays.asList(ResourceType.DEVICE, ResourceType.INTEGRATION));
        permissionDTO.setHaveAllPermissions(userResourceDTO.isHasAllResource());
        permissionDTO.setIds(new ArrayList<>());

        if (!userResourceDTO.isHasAllResource()) {
            List<String> deviceIds = new ArrayList<>();
            Map<ResourceType, List<String>> resource = userResourceDTO.getResource();
            if (resource != null && !resource.isEmpty()) {
                resource.forEach((resourceType, resourceIds) -> {
                    if (resourceType == ResourceType.DEVICE) {
                        deviceIds.addAll(resourceIds);
                    } else if (resourceType == ResourceType.INTEGRATION) {
                        List<String> integrationDeviceIds = deviceFacade.getDeviceNameByIntegrations(resourceIds).stream()
                                .map(t -> String.valueOf(t.getId()))
                                .toList();
                        deviceIds.addAll(integrationDeviceIds);
                    }
                });
            }

            permissionDTO.setIds(deviceIds);
        }
        return permissionDTO;
    }

}
