package com.milesight.beaveriot.permission.service;

import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.entity.facade.IEntityFacade;
import com.milesight.beaveriot.permission.dto.PermissionDTO;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import com.milesight.beaveriot.permission.facade.IPermissionFacade;
import com.milesight.beaveriot.user.dto.UserResourceDTO;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.facade.IUserFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author loong
 * @date 2024/11/22 9:17
 */
@Service
public class EntityPermissionService {

    @Autowired
    private IUserFacade userFacade;

    @Autowired
    private IEntityFacade entityFacade;

    @Autowired
    private IDeviceFacade deviceFacade;

    @Autowired
    private IPermissionFacade permissionFacade;

    public PermissionDTO getEntityPermission(Long userId) {
        PermissionDTO permissionDTO = new PermissionDTO();
        UserResourceDTO userResourceDTO = userFacade.getResource(userId, Arrays.asList(ResourceType.ENTITY, ResourceType.DEVICE, ResourceType.INTEGRATION));
        permissionDTO.setHaveAllPermissions(userResourceDTO.isHasAllResource());
        permissionDTO.setIds(new ArrayList<>());

        if (!userResourceDTO.isHasAllResource()) {
            Map<ResourceType, List<String>> resource = userResourceDTO.getResource();
            Set<Long> entityIds = new HashSet<>();
            List<String> attachTargetIds = new ArrayList<>();
            boolean hasEntityCustomViewPermission = permissionFacade.hasMenuPermission(OperationPermissionCode.ENTITY_CUSTOM_VIEW);
            if (hasEntityCustomViewPermission) {
                attachTargetIds.add(IntegrationConstants.SYSTEM_INTEGRATION_ID);
            }
            if (resource != null && !resource.isEmpty()) {
                resource.forEach((resourceType, resourceIds) -> {
                    switch (resourceType) {
                        case ENTITY -> resourceIds.stream().map(Long::valueOf).forEach(entityIds::add);
                        case DEVICE -> attachTargetIds.addAll(resourceIds);
                        case INTEGRATION -> {
                            attachTargetIds.addAll(resourceIds);
                            List<String> deviceIds = deviceFacade.getDeviceNameByIntegrations(resourceIds).stream()
                                    .map(t -> String.valueOf(t.getId()))
                                    .toList();
                            attachTargetIds.addAll(deviceIds);
                        }
                        default -> {
                            // skip
                        }
                    }
                });
            }

            if (!entityIds.isEmpty()) {
                attachTargetIds.addAll(entityFacade.mapEntityIdToAttachTargetId(entityIds).values());
            }

            permissionDTO.setIds(attachTargetIds);
        }
        return permissionDTO;
    }
}
