package com.milesight.beaveriot.permission.service;

import com.milesight.beaveriot.context.constants.CacheKeyConstants;
import com.milesight.beaveriot.permission.dto.IntegrationPermissionDTO;
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
 * @date 2024/11/28 17:14
 */
@Service
public class IntegrationPermissionService {

    @Autowired
    IUserFacade userFacade;

    @Cacheable(cacheNames = CacheKeyConstants.INTEGRATION_PERMISSION_CACHE_NAME_PREFIX, key = "#p0")
    public IntegrationPermissionDTO getIntegrationPermission(Long userId) {
        IntegrationPermissionDTO integrationPermissionDTO = new IntegrationPermissionDTO();
        UserResourceDTO userResourceDTO = userFacade.getResource(userId, Arrays.asList(ResourceType.INTEGRATION));
        integrationPermissionDTO.setHasAllPermission(userResourceDTO.isHasAllResource());
        integrationPermissionDTO.setIntegrationIds(new ArrayList<>());
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
            integrationPermissionDTO.setIntegrationIds(integrationIds);
        }
        return integrationPermissionDTO;
    }
}
