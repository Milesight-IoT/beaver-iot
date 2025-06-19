package com.milesight.beaveriot.permission.service;

import com.milesight.beaveriot.permission.dto.PermissionDTO;
import com.milesight.beaveriot.user.dto.UserResourceDTO;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.facade.IUserFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author loong
 * @date 2024/12/13 10:18
 */
@Service
public class WorkflowPermissionService {

    @Autowired
    IUserFacade userFacade;

    public PermissionDTO getWorkflowPermission(Long userId) {
        PermissionDTO permissionDTO = new PermissionDTO();
        UserResourceDTO userResourceDTO = userFacade.getResource(userId, Collections.singletonList(ResourceType.WORKFLOW));
        permissionDTO.setHaveAllPermissions(userResourceDTO.isHasAllResource());
        permissionDTO.setIds(new ArrayList<>());
        if (!userResourceDTO.isHasAllResource()) {
            List<String> workflowIds = new ArrayList<>();
            Map<ResourceType, List<String>> resource = userResourceDTO.getResource();
            if (resource != null && !resource.isEmpty()) {
                resource.forEach((resourceType, resourceIds) -> {
                    if (resourceType == ResourceType.WORKFLOW) {
                        workflowIds.addAll(resourceIds);
                    }
                });
            }
            permissionDTO.setIds(workflowIds);
        }
        return permissionDTO;
    }
}
