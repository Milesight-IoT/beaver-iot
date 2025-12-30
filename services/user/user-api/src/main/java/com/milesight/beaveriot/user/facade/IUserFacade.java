package com.milesight.beaveriot.user.facade;

import com.milesight.beaveriot.user.dto.MenuDTO;
import com.milesight.beaveriot.user.dto.RoleDTO;
import com.milesight.beaveriot.user.dto.TenantDTO;
import com.milesight.beaveriot.user.dto.UserDTO;
import com.milesight.beaveriot.user.dto.UserResourceDTO;
import com.milesight.beaveriot.user.enums.ResourceType;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/14 11:47
 */
public interface IUserFacade {

    TenantDTO analyzeTenantId(String tenantId);

    List<TenantDTO> getAllTenants();

    UserDTO getEnableUserByEmail(String email);

    UserResourceDTO getResource(Long userId, List<ResourceType> resourceTypes);

    List<UserDTO> getUserByIds(List<Long> userIds);

    List<RoleDTO> getRolesByUserId(Long userId);

    List<MenuDTO> getMenusByUserId(Long userId);

    void deleteResource(ResourceType resourceType, List<Long> resourceIds);

    void associateResource(Long userId, ResourceType resourceType, List<Long> resourceIds);

    List<UserDTO> getUserLike(String keyword);

    boolean isSuperAdmin(Long userId);
}
