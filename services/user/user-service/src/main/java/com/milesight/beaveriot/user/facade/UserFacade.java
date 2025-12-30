package com.milesight.beaveriot.user.facade;

import com.milesight.beaveriot.context.constants.CacheKeyConstants;
import com.milesight.beaveriot.user.constants.UserConstants;
import com.milesight.beaveriot.user.convert.TenantConverter;
import com.milesight.beaveriot.user.convert.UserConverter;
import com.milesight.beaveriot.user.dto.MenuDTO;
import com.milesight.beaveriot.user.dto.RoleDTO;
import com.milesight.beaveriot.user.dto.TenantDTO;
import com.milesight.beaveriot.user.dto.UserDTO;
import com.milesight.beaveriot.user.dto.UserResourceDTO;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.enums.UserStatus;
import com.milesight.beaveriot.user.model.Menu;
import com.milesight.beaveriot.user.po.RoleMenuPO;
import com.milesight.beaveriot.user.po.RolePO;
import com.milesight.beaveriot.user.po.TenantPO;
import com.milesight.beaveriot.user.po.UserPO;
import com.milesight.beaveriot.user.po.UserRolePO;
import com.milesight.beaveriot.user.repository.RoleMenuRepository;
import com.milesight.beaveriot.user.repository.RoleRepository;
import com.milesight.beaveriot.user.repository.TenantRepository;
import com.milesight.beaveriot.user.repository.UserRepository;
import com.milesight.beaveriot.user.repository.UserRoleRepository;
import com.milesight.beaveriot.user.service.RoleService;
import com.milesight.beaveriot.user.service.UserService;
import com.milesight.beaveriot.user.util.MenuStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author loong
 * @date 2024/10/14 11:47
 */
@Service
public class UserFacade implements IUserFacade {

    @Autowired
    private UserService userService;

    @Lazy
    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleMenuRepository roleMenuRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Override
    public TenantDTO analyzeTenantId(String tenantId) {
        TenantPO tenantPO = userService.analyzeTenantId(tenantId);
        return TenantConverter.INSTANCE.convertDTO(tenantPO);
    }

    @Override
    public List<TenantDTO> getAllTenants() {
        List<TenantPO> tenantPOList = tenantRepository.findAll();
        return TenantConverter.INSTANCE.convertDTOList(tenantPOList);
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
        return userService.getUserResource(userId, resourceTypes);
    }

    @Override
    public List<UserDTO> getUserByIds(List<Long> userIds) {
        List<UserPO> userPOs = userService.getUserByIds(userIds);
        return UserConverter.INSTANCE.convertDTOList(userPOs);
    }

    @Override
    public List<RoleDTO> getRolesByUserId(Long userId) {
        List<RolePO> rolePOs = getRolePOsByUserId(userId);
        if (rolePOs == null || rolePOs.isEmpty()) {
            return new ArrayList<>();
        }
        return rolePOs.stream()
                .map(rolePO -> {
                    RoleDTO roleDTO = new RoleDTO();
                    roleDTO.setRoleId(rolePO.getId());
                    roleDTO.setRoleName(rolePO.getName());
                    return roleDTO;
                })
                .collect(Collectors.toList());
    }

    private List<RolePO> getRolePOsByUserId(Long userId) {
        List<UserRolePO> userRolePOList = userRoleRepository.findAll(filterable -> filterable.eq(UserRolePO.Fields.userId, userId));
        if (userRolePOList == null || userRolePOList.isEmpty()) {
            return new ArrayList<>();
        }
        Long[] roleIds = userRolePOList.stream().map(UserRolePO::getRoleId).toArray(Long[]::new);
        return roleRepository.findAll(filterable -> filterable.in(RolePO.Fields.id, roleIds));
    }

    @Cacheable(cacheNames = CacheKeyConstants.USER_ID_TO_MENUS, key = "T(com.milesight.beaveriot.context.security.TenantContext).getTenantId()+':'+#p0")
    @Override
    public List<MenuDTO> getMenusByUserId(Long userId) {
        List<RolePO> rolePOs = getRolePOsByUserId(userId);
        if (rolePOs == null || rolePOs.isEmpty()) {
            return new ArrayList<>();
        }

        boolean isSuperAdmin = rolePOs.stream().anyMatch(rolePO -> Objects.equals(rolePO.getName(), UserConstants.SUPER_ADMIN_ROLE_NAME));
        if (isSuperAdmin) {
            List<Menu> menuList = MenuStore.getAllMenus();
            return menuList.stream()
                    .map(menuPO -> {
                        MenuDTO menuDTO = new MenuDTO();
                        menuDTO.setMenuId(menuPO.getId());
                        menuDTO.setMenuCode(menuPO.getCode());
                        return menuDTO;
                    })
                    .collect(Collectors.toList());
        }

        Long[] roleIds = rolePOs.stream().map(RolePO::getId).toArray(Long[]::new);
        List<RoleMenuPO> roleMenuPOs = roleMenuRepository.findAll(filterable -> filterable.in(RoleMenuPO.Fields.roleId, roleIds));
        if (roleMenuPOs == null || roleMenuPOs.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> menuIds = roleMenuPOs.stream().map(RoleMenuPO::getMenuId).collect(Collectors.toSet());
        return MenuStore.getAllMenus()
                .stream()
                .filter(menuPO -> menuIds.contains(menuPO.getId()))
                .map(menuPO -> {
                    MenuDTO menuDTO = new MenuDTO();
                    menuDTO.setMenuId(menuPO.getId());
                    menuDTO.setMenuCode(menuPO.getCode());
                    return menuDTO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean isSuperAdmin(Long userId) {
        List<RolePO> rolePOs = getRolePOsByUserId(userId);
        if (CollectionUtils.isEmpty(rolePOs)) {
            return false;
        }

        return rolePOs.stream().anyMatch(rolePO -> Objects.equals(rolePO.getName(), UserConstants.SUPER_ADMIN_ROLE_NAME));
    }

    @Override
    public void deleteResource(ResourceType resourceType, List<Long> resourceIds) {
        roleService.deleteResource(resourceType, resourceIds);
    }

    @Override
    public void associateResource(Long userId, ResourceType resourceType, List<Long> resourceIds) {
        roleService.associateResource(userId, resourceType, resourceIds);
    }

    @Override
    public List<UserDTO> getUserLike(String keyword) {
        List<UserPO> userPOs = userRepository.findAll(filterable -> filterable.or(filterable1 -> filterable1.likeIgnoreCase(StringUtils.hasText(keyword), UserPO.Fields.nickname, keyword)
                .likeIgnoreCase(StringUtils.hasText(keyword), UserPO.Fields.email, keyword)));
        return UserConverter.INSTANCE.convertDTOList(userPOs);
    }

}
