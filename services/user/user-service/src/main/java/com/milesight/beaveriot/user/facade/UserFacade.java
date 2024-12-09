package com.milesight.beaveriot.user.facade;

import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.user.convert.TenantConverter;
import com.milesight.beaveriot.user.convert.UserConverter;
import com.milesight.beaveriot.user.dto.MenuDTO;
import com.milesight.beaveriot.user.dto.RoleDTO;
import com.milesight.beaveriot.user.dto.TenantDTO;
import com.milesight.beaveriot.user.dto.UserDTO;
import com.milesight.beaveriot.user.dto.UserResourceDTO;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.enums.UserStatus;
import com.milesight.beaveriot.user.po.MenuPO;
import com.milesight.beaveriot.user.po.RoleMenuPO;
import com.milesight.beaveriot.user.po.RolePO;
import com.milesight.beaveriot.user.po.RoleResourcePO;
import com.milesight.beaveriot.user.po.TenantPO;
import com.milesight.beaveriot.user.po.UserPO;
import com.milesight.beaveriot.user.po.UserRolePO;
import com.milesight.beaveriot.user.repository.MenuRepository;
import com.milesight.beaveriot.user.repository.RoleMenuRepository;
import com.milesight.beaveriot.user.repository.RoleRepository;
import com.milesight.beaveriot.user.repository.RoleResourceRepository;
import com.milesight.beaveriot.user.repository.UserRoleRepository;
import com.milesight.beaveriot.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author loong
 * @date 2024/10/14 11:47
 */
@Service
public class UserFacade implements IUserFacade {

    @Autowired
    UserService userService;
    @Autowired
    UserRoleRepository userRoleRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    RoleMenuRepository roleMenuRepository;
    @Autowired
    MenuRepository menuRepository;
    @Autowired
    RoleResourceRepository roleResourceRepository;

    @Override
    public TenantDTO analyzeTenantId(Long tenantId) {
        TenantPO tenantPO = userService.analyzeTenantId(tenantId);
        return TenantConverter.INSTANCE.convertDTO(tenantPO);
    }

    @Override
    public UserDTO getEnableUserByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        UserPO userPO = userService.getUserByEmail(email, UserStatus.ENABLE);
        return UserConverter.INSTANCE.convertDTO(userPO);
    }

    @Override
    public UserResourceDTO getResource(Long userId, List<ResourceType> resourceTypes) {
        return userService.getResource(userId, resourceTypes);
    }

    @Override
    public List<UserDTO> getUserByIds(List<Long> userIds) {
        List<UserPO> userPOS = userService.getUserByIds(userIds);
        return UserConverter.INSTANCE.convertDTOList(userPOS);
    }

    @Override
    public List<RoleDTO> getRolesByUserId(Long userId) {
        List<UserRolePO> userRolePOList = userRoleRepository.findAll(filterable -> filterable.eq(UserRolePO.Fields.userId, userId));
        if (userRolePOList == null || userRolePOList.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> roleIds = userRolePOList.stream().map(UserRolePO::getId).toList();
        List<RolePO> rolePOS = roleRepository.findAll(filterable -> filterable.in(RolePO.Fields.id, roleIds.toArray()));
        if (rolePOS == null || rolePOS.isEmpty()) {
            return new ArrayList<>();
        }
        return rolePOS.stream().map(rolePO -> {
            RoleDTO roleDTO = new RoleDTO();
            roleDTO.setRoleId(rolePO.getId());
            roleDTO.setRoleName(rolePO.getName());
            return roleDTO;
        }).toList();
    }

    @Override
    public List<MenuDTO> getMenusByUserId(Long userId) {
        List<UserRolePO> userRolePOList = userRoleRepository.findAll(filterable -> filterable.eq(UserRolePO.Fields.userId, userId));
        if (userRolePOList == null || userRolePOList.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> roleIds = userRolePOList.stream().map(UserRolePO::getId).toList();
        List<RoleMenuPO> roleMenuPOS = roleMenuRepository.findAll(filterable -> filterable.in(RoleMenuPO.Fields.roleId, roleIds.toArray()));
        if (roleMenuPOS == null || roleMenuPOS.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> menuIds = roleMenuPOS.stream().map(RoleMenuPO::getMenuId).toList();
        List<MenuPO> menuPOList = menuRepository.findAll(filterable -> filterable.in(MenuPO.Fields.id, menuIds.toArray()));
        if (menuPOList == null || menuPOList.isEmpty()) {
            return new ArrayList<>();
        }
        return menuPOList.stream().map(menuPO -> {
            MenuDTO menuDTO = new MenuDTO();
            menuDTO.setMenuId(menuPO.getId());
            menuDTO.setMenuCode(menuPO.getCode());
            return menuDTO;
        }).toList();
    }

    @Override
    public void deleteResource(ResourceType resourceType, List<Long> resourceIds) {
        List<RoleResourcePO> roleResourcePOS = roleResourceRepository.findAll(filterable -> filterable.in(RoleResourcePO.Fields.resourceId, resourceIds.toArray()).eq(RoleResourcePO.Fields.resourceType, resourceType));
        if (roleResourcePOS != null && !roleResourcePOS.isEmpty()) {
            roleResourceRepository.deleteAll(roleResourcePOS);
        }
    }

    @Override
    public void associateResource(Long userId, ResourceType resourceType, List<Long> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()){
            return;
        }
        List<UserRolePO> userRolePOList = userRoleRepository.findAll(filterable -> filterable.eq(UserRolePO.Fields.userId, userId));
        if (userRolePOList == null || userRolePOList.isEmpty()) {
            return;
        }
        List<RoleResourcePO> roleResourcePOS = new ArrayList<>();
        userRolePOList.forEach(userRolePO -> {
            Long roleId = userRolePO.getRoleId();
            resourceIds.forEach(resourceId -> {
                RoleResourcePO roleResourcePO = new RoleResourcePO();
                roleResourcePO.setId(SnowflakeUtil.nextId());
                roleResourcePO.setRoleId(roleId);
                roleResourcePO.setResourceId(resourceId.toString());
                roleResourcePO.setResourceType(resourceType);
                roleResourcePOS.add(roleResourcePO);
            });
        });
        roleResourceRepository.saveAll(roleResourcePOS);
    }

}
