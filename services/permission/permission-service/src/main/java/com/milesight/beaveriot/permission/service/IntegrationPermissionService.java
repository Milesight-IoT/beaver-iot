package com.milesight.beaveriot.permission.service;

import com.milesight.beaveriot.permission.dto.PermissionDTO;
import com.milesight.beaveriot.user.dto.UserResourceDTO;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.facade.IUserFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author loong
 * @date 2024/11/28 17:14
 */
@Service
public class IntegrationPermissionService {

    @Autowired
    private IUserFacade userFacade;

    public PermissionDTO getIntegrationPermission(Long userId) {
        PermissionDTO permissionDTO = new PermissionDTO();
        UserResourceDTO userResourceDTO = userFacade.getResource(userId, List.of(ResourceType.INTEGRATION));
        permissionDTO.setHaveAllPermissions(userResourceDTO.isHasAllResource());
        permissionDTO.setIds(new ArrayList<>());

        if (!userResourceDTO.isHasAllResource()) {
            List<String> integrationIds = new ArrayList<>();
            Map<ResourceType, List<String>> resource = userResourceDTO.getResource();
            if (resource != null && !resource.isEmpty()) {
                resource.forEach((resourceType, resourceIds) -> {
                    if (resourceType == ResourceType.INTEGRATION) {
                        integrationIds.addAll(resourceIds);
                    }
                });
            }
            permissionDTO.setIds(integrationIds);
        }

        return permissionDTO;
    }
}
