package com.milesight.beaveriot.permission.service;

import com.milesight.beaveriot.dashboard.facade.IDashboardFacade;
import com.milesight.beaveriot.permission.dto.DashboardPermissionDTO;
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
 * @date 2024/11/26 11:09
 */
@Service
public class DashboardPermissionService {

    @Autowired
    IUserFacade userFacade;
    @Autowired
    IDashboardFacade dashboardFacade;

    public DashboardPermissionDTO getDashboardPermission(Long userId) {
        DashboardPermissionDTO dashboardPermissionDTO = new DashboardPermissionDTO();
        UserResourceDTO userResourceDTO = userFacade.getResource(userId, Collections.singletonList(ResourceType.DASHBOARD));
        dashboardPermissionDTO.setHasAllPermission(userResourceDTO.isHasAllResource());
        dashboardPermissionDTO.setDashboardIds(new ArrayList<>());
        if (!userResourceDTO.isHasAllResource()) {
            List<String> dashboardIds = new ArrayList<>();
            Map<ResourceType, List<String>> resource = userResourceDTO.getResource();
            if (resource != null && !resource.isEmpty()) {
                resource.forEach((resourceType, resourceIds) -> {
                    if (resourceType == ResourceType.DASHBOARD) {
                        dashboardIds.addAll(resourceIds);
                    }
                });
            }
            dashboardPermissionDTO.setDashboardIds(dashboardIds);
        }
        return dashboardPermissionDTO;
    }

}
