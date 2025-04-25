package com.milesight.beaveriot.permission.service;

import com.milesight.beaveriot.context.constants.CacheKeyConstants;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.device.dto.DeviceNameDTO;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.entity.dto.EntityDTO;
import com.milesight.beaveriot.entity.facade.IEntityFacade;
import com.milesight.beaveriot.permission.dto.EntityPermissionDTO;
import com.milesight.beaveriot.user.dto.UserResourceDTO;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.facade.IUserFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author loong
 * @date 2024/11/22 9:17
 */
@Service
public class EntityPermissionService {

    @Autowired
    IUserFacade userFacade;
    @Autowired
    IEntityFacade entityFacade;
    @Autowired
    IDeviceFacade deviceFacade;

    @Cacheable(cacheNames = CacheKeyConstants.ENTITY_PERMISSION_CACHE_NAME_PREFIX, key = "#p0")
    public EntityPermissionDTO getEntityPermission(Long userId) {
        EntityPermissionDTO entityPermissionDTO = new EntityPermissionDTO();
        UserResourceDTO userResourceDTO = userFacade.getResource(userId, Arrays.asList(ResourceType.ENTITY, ResourceType.DEVICE, ResourceType.INTEGRATION));
        entityPermissionDTO.setHasAllPermission(userResourceDTO.isHasAllResource());
        entityPermissionDTO.setEntityIds(new ArrayList<>());
        if (!userResourceDTO.isHasAllResource()) {
            List<String> targetIds = new ArrayList<>();
            targetIds.add(IntegrationConstants.SYSTEM_INTEGRATION_ID);
            List<String> resourceEntityIds = new ArrayList<>();
            Map<ResourceType, List<String>> resource = userResourceDTO.getResource();
            if (resource != null && !resource.isEmpty()) {
                resource.forEach((resourceType, resourceIds) -> {
                    if (resourceType == ResourceType.ENTITY) {
                        resourceEntityIds.addAll(resourceIds);
                    } else if (resourceType == ResourceType.DEVICE) {
                        targetIds.addAll(resourceIds);
                    } else if (resourceType == ResourceType.INTEGRATION) {
                        targetIds.addAll(resourceIds);
                        List<DeviceNameDTO> integrationDevices = deviceFacade.getDeviceNameByIntegrations(resourceIds);
                        if (integrationDevices != null && !integrationDevices.isEmpty()) {
                            List<String> deviceIds = integrationDevices.stream().map(t -> String.valueOf(t.getId())).toList();
                            targetIds.addAll(deviceIds);
                        }
                    }
                });
            }
            List<EntityDTO> entityDTOList = entityFacade.getTargetEntities(targetIds);
            if (entityDTOList != null && !entityDTOList.isEmpty()) {
                resourceEntityIds.addAll(entityDTOList.stream().map(EntityDTO::getEntityId).map(Object::toString).toList());
            }
            entityPermissionDTO.setEntityIds(resourceEntityIds);
        }
        return entityPermissionDTO;
    }
}
