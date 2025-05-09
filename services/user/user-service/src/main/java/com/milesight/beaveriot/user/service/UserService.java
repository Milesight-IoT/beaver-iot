package com.milesight.beaveriot.user.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.GenericQueryPageRequest;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.aspect.SecurityUserContext;
import com.milesight.beaveriot.context.security.SecurityUser;
import com.milesight.beaveriot.device.dto.DeviceNameDTO;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.user.constants.UserConstants;
import com.milesight.beaveriot.user.dto.UserResourceDTO;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.enums.UserErrorCode;
import com.milesight.beaveriot.user.enums.UserStatus;
import com.milesight.beaveriot.user.model.request.BatchDeleteUserRequest;
import com.milesight.beaveriot.user.model.request.ChangePasswordRequest;
import com.milesight.beaveriot.user.model.request.CreateUserRequest;
import com.milesight.beaveriot.user.model.request.UpdatePasswordRequest;
import com.milesight.beaveriot.user.model.request.UpdateUserRequest;
import com.milesight.beaveriot.user.model.request.UserPermissionRequest;
import com.milesight.beaveriot.user.model.request.UserRegisterRequest;
import com.milesight.beaveriot.user.model.response.MenuResponse;
import com.milesight.beaveriot.user.model.response.UserInfoResponse;
import com.milesight.beaveriot.user.model.response.UserMenuResponse;
import com.milesight.beaveriot.user.model.response.UserPermissionResponse;
import com.milesight.beaveriot.user.model.response.UserStatusResponse;
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
import com.milesight.beaveriot.user.repository.TenantRepository;
import com.milesight.beaveriot.user.repository.UserRepository;
import com.milesight.beaveriot.user.repository.UserRoleRepository;
import com.milesight.beaveriot.user.util.SignUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author loong
 * @date 2024/10/14 8:42
 */
@Service
public class UserService {

    @Autowired
    UserRepository userRepository;
    @Autowired
    UserRoleRepository userRoleRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    RoleResourceRepository roleResourceRepository;
    @Autowired
    RoleMenuRepository roleMenuRepository;
    @Autowired
    MenuRepository menuRepository;
    @Autowired
    TenantRepository tenantRepository;
    @Autowired
    @Lazy
    IDeviceFacade deviceFacade;

    @SecurityUserContext(tenantId = "#tenantId")
    @Transactional(rollbackFor = Exception.class)
    public void register(String tenantId, UserRegisterRequest userRegisterRequest) {
        String email = userRegisterRequest.getEmail();
        String nickname = userRegisterRequest.getNickname();
        String password = userRegisterRequest.getPassword();
        if (!StringUtils.hasText(email) || !StringUtils.hasText(nickname) || !StringUtils.hasText(password)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("email and nickname and password must be not null").build();
        }
        UserPO userPO = getUserByEmail(email);
        if (userPO != null) {
            throw ServiceException.with(UserErrorCode.USER_REGISTER_EMAIL_EXIST).build();
        }
        userPO = new UserPO();
        userPO.setId(SnowflakeUtil.nextId());
        userPO.setEmail(email);
        userPO.setEmailHash(SignUtils.sha256Hex(email));
        userPO.setNickname(nickname);
        userPO.setPassword(new BCryptPasswordEncoder().encode(password));
        userPO.setPreference(null);
        userPO.setStatus(UserStatus.ENABLE);
        userRepository.save(userPO);

        Long roleId = analyzeSuperAdminRoleId();
        UserRolePO userRolePO = new UserRolePO();
        userRolePO.setId(SnowflakeUtil.nextId());
        userRolePO.setUserId(userPO.getId());
        userRolePO.setRoleId(roleId);
        userRoleRepository.save(userRolePO);
    }

    @SecurityUserContext(tenantId = "#tenantId")
    public UserStatusResponse status(String tenantId) {
        List<UserPO> users = userRepository.findAll();
        boolean isInit = users != null && !users.isEmpty();
        UserStatusResponse userStatusResponse = new UserStatusResponse();
        userStatusResponse.setInit(isInit);
        return userStatusResponse;
    }

    public UserInfoResponse getUserInfo() {
        SecurityUser securityUser = com.milesight.beaveriot.context.security.SecurityUserContext.getSecurityUser();
        UserInfoResponse userInfoResponse = new UserInfoResponse();
        if (securityUser == null){
            return userInfoResponse;
        }
        Long userId = securityUser.getUserId();
        userInfoResponse.setUserId(userId.toString());
        userInfoResponse.setTenantId(securityUser.getTenantId());
        userInfoResponse.setNickname(securityUser.getNickname());
        userInfoResponse.setEmail(securityUser.getEmail());
        userInfoResponse.setCreatedAt(securityUser.getCreatedAt());

        List<MenuPO> menuPOS = menuRepository.findAll();
        if (menuPOS != null && !menuPOS.isEmpty()) {
            List<UserMenuResponse> userMenuResponses = getMenusByUserId(userId);
            List<String> userMenuParentIds = userMenuResponses.stream().map(UserMenuResponse::getParentId).filter(Objects::nonNull).distinct().toList();
            List<UserMenuResponse> allUserMenuResponses = new ArrayList<>();
            allUserMenuResponses.addAll(userMenuResponses);
            recurrenceParentMenu(menuPOS, userMenuParentIds, allUserMenuResponses);

            List<MenuResponse> menuResponses = new ArrayList<>();
            List<UserMenuResponse> rootUserMenuResponses = allUserMenuResponses.stream().filter(userMenuResponse -> userMenuResponse.getParentId() == null).distinct().toList();
            rootUserMenuResponses.forEach(UserMenuResponse -> {
                MenuResponse menuResponse = new MenuResponse();
                menuResponse.setMenuId(UserMenuResponse.getMenuId());
                menuResponse.setCode(UserMenuResponse.getCode());
                menuResponse.setName(UserMenuResponse.getName());
                menuResponse.setType(UserMenuResponse.getType());
                menuResponse.setParentId(UserMenuResponse.getParentId());

                List<MenuResponse> menuChild = recurrenceChildMenu(allUserMenuResponses, UserMenuResponse.getMenuId());
                menuResponse.setChildren(menuChild);
                menuResponses.add(menuResponse);
            });

            userInfoResponse.setMenus(menuResponses);
        }

        AtomicBoolean isSuperAdmin = new AtomicBoolean(false);
        List<UserRolePO> userRolePOS = userRoleRepository.findAll(filter -> filter.eq(UserRolePO.Fields.userId, userId));
        if (userRolePOS != null && !userRolePOS.isEmpty()) {
            List<Long> roleIds = userRolePOS.stream().map(UserRolePO::getRoleId).toList();
            List<RolePO> rolePOS = roleRepository.findAll(filter -> filter.in(RolePO.Fields.id, roleIds.toArray()));
            List<UserInfoResponse.Role> roles = rolePOS.stream().map(rolePO -> {
                if (rolePO.getName().equals(UserConstants.SUPER_ADMIN_ROLE_NAME)) {
                    isSuperAdmin.set(true);
                }
                UserInfoResponse.Role role = new UserInfoResponse.Role();
                role.setRoleId(rolePO.getId().toString());
                role.setRoleName(rolePO.getName());
                return role;
            }).collect(Collectors.toList());
            userInfoResponse.setRoles(roles);
        }
        userInfoResponse.setIsSuperAdmin(isSuperAdmin.get());
        return userInfoResponse;
    }

    private void recurrenceParentMenu(List<MenuPO> menuPOS, List<String> userMenuParentIds, List<UserMenuResponse> allUserMenuResponses) {
        if (userMenuParentIds == null || userMenuParentIds.isEmpty()) {
            return;
        }
        List<MenuPO> parentMenuPOS = menuPOS.stream().filter(t -> userMenuParentIds.contains(t.getId().toString())).toList();
        if (parentMenuPOS.isEmpty()) {
            return;
        }
        List<UserMenuResponse> parentUserMenuResponses = parentMenuPOS.stream().map(menuPO -> {
            UserMenuResponse userMenuResponse = new UserMenuResponse();
            userMenuResponse.setMenuId(menuPO.getId().toString());
            userMenuResponse.setCode(menuPO.getCode());
            userMenuResponse.setName(menuPO.getName());
            userMenuResponse.setType(menuPO.getType());
            userMenuResponse.setParentId(menuPO.getParentId() == null ? null : menuPO.getParentId().toString());
            return userMenuResponse;
        }).toList();
        allUserMenuResponses.addAll(parentUserMenuResponses);

        List<String> parentMenuParentIds = parentMenuPOS.stream().map(MenuPO::getParentId).filter(Objects::nonNull).map(Objects::toString).distinct().toList();

        recurrenceParentMenu(menuPOS, parentMenuParentIds, allUserMenuResponses);
    }

    private List<MenuResponse> recurrenceChildMenu(List<UserMenuResponse> menuResponses, String parentId) {
        List<UserMenuResponse> menuChs = menuResponses.stream().filter(t -> t.getParentId() != null && t.getParentId().equals(parentId)).distinct().toList();
        if (menuChs.isEmpty()) {
            return new ArrayList<>();
        }
        List<MenuResponse> menuChResponses = new ArrayList<>();
        menuChs.forEach(t -> {
            MenuResponse menuChResponse = new MenuResponse();
            menuChResponse.setMenuId(t.getMenuId());
            menuChResponse.setCode(t.getCode());
            menuChResponse.setName(t.getName());
            menuChResponse.setType(t.getType());
            menuChResponse.setParentId(t.getParentId());
            List<MenuResponse> menuChild = recurrenceChildMenu(menuResponses, t.getMenuId());
            menuChResponse.setChildren(menuChild);
            menuChResponses.add(menuChResponse);
        });
        return menuChResponses;
    }

    public Page<UserInfoResponse> getUsers(GenericQueryPageRequest userListRequest) {
        if (userListRequest.getSort().getOrders().isEmpty()) {
            userListRequest.sort(new Sorts().desc(UserPO.Fields.id));
        }
        String keyword = userListRequest.getKeyword();
        Page<UserPO> userPages = userRepository.findAll(filterable -> filterable.or(filterable1 -> filterable1.likeIgnoreCase(StringUtils.hasText(keyword), UserPO.Fields.nickname, keyword)
                                .likeIgnoreCase(StringUtils.hasText(keyword), UserPO.Fields.email, keyword))
                , userListRequest.toPageable());
        if (userPages == null || userPages.getContent().isEmpty()) {
            return Page.empty();
        }
        Map<Long, List<Long>> userRoleIdMap = new HashMap<>();
        Map<Long, String> roleNameMap = new HashMap<>();
        List<Long> userIds = userPages.stream().map(UserPO::getId).toList();
        List<UserRolePO> userRolePOS = userRoleRepository.findAll(filter -> filter.in(UserRolePO.Fields.userId, userIds.toArray()));
        if (userRolePOS != null && !userRolePOS.isEmpty()) {
            userRoleIdMap.putAll(userRolePOS.stream().collect(Collectors.groupingBy(UserRolePO::getUserId, Collectors.mapping(UserRolePO::getRoleId, Collectors.toList()))));
            List<Long> roleIds = userRolePOS.stream().map(UserRolePO::getRoleId).toList();
            List<RolePO> rolePOS = roleRepository.findAll(filter -> filter.in(RolePO.Fields.id, roleIds.toArray()));
            if (rolePOS != null && !rolePOS.isEmpty()) {
                roleNameMap.putAll(rolePOS.stream().collect(Collectors.toMap(RolePO::getId, RolePO::getName)));
            }
        }
        return userPages.map(userPO -> {
            UserInfoResponse userInfoResponse = new UserInfoResponse();
            userInfoResponse.setUserId(userPO.getId().toString());
            userInfoResponse.setTenantId(userPO.getTenantId());
            userInfoResponse.setNickname(userPO.getNickname());
            userInfoResponse.setEmail(userPO.getEmail());
            userInfoResponse.setCreatedAt(userPO.getCreatedAt().toString());
            List<Long> roleIds = userRoleIdMap.get(userPO.getId());
            if (roleIds != null && !roleIds.isEmpty()) {
                List<UserInfoResponse.Role> roles = new ArrayList<>();
                roleIds.forEach(roleId -> {
                    UserInfoResponse.Role role = new UserInfoResponse.Role();
                    role.setRoleId(roleId.toString());
                    role.setRoleName(roleNameMap.get(roleId));
                    roles.add(role);
                });
                userInfoResponse.setRoles(roles);
            }
            return userInfoResponse;
        });
    }

    public void createUser(CreateUserRequest createUserRequest) {
        String email = createUserRequest.getEmail();
        String nickname = createUserRequest.getNickname();
        String password = createUserRequest.getPassword();
        if (!StringUtils.hasText(email) || !StringUtils.hasText(nickname) || !StringUtils.hasText(password)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("email and nickname and password must be not null").build();
        }
        UserPO userPO = getUserByEmail(email);
        if (userPO != null) {
            throw ServiceException.with(UserErrorCode.USER_REGISTER_EMAIL_EXIST).build();
        }
        userPO = new UserPO();
        userPO.setId(SnowflakeUtil.nextId());
        userPO.setEmail(email);
        userPO.setEmailHash(SignUtils.sha256Hex(email));
        userPO.setNickname(nickname);
        userPO.setPassword(new BCryptPasswordEncoder().encode(password));
        userPO.setPreference(null);
        userPO.setStatus(UserStatus.ENABLE);
        userRepository.save(userPO);
    }

    public void updateUser(Long userId, UpdateUserRequest updateUserRequest) {
        UserPO userPO = userRepository.findOne(filter -> filter.eq(UserPO.Fields.id, userId)).orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).build());
        userPO.setNickname(updateUserRequest.getNickname());
        userPO.setEmail(updateUserRequest.getEmail());
        userRepository.save(userPO);
    }

    public void changePassword(Long userId, ChangePasswordRequest changePasswordRequest) {
        String password = changePasswordRequest.getPassword();
        if (!StringUtils.hasText(password)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("password must be not null").build();
        }
        UserPO userPO = userRepository.findOne(filter -> filter.eq(UserPO.Fields.id, userId)).orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).build());
        userPO.setPassword(new BCryptPasswordEncoder().encode(password));
        userRepository.save(userPO);
    }

    public void updatePassword(UpdatePasswordRequest updatePasswordRequest) {
        UserPO userPO = userRepository.findOne(filter -> filter.eq(UserPO.Fields.id, com.milesight.beaveriot.context.security.SecurityUserContext.getUserId())).orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).build());
        if (!new BCryptPasswordEncoder().matches(updatePasswordRequest.getOldPassword(), userPO.getPassword())) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("old password is not correct").build();
        }
        userPO.setPassword(new BCryptPasswordEncoder().encode(updatePasswordRequest.getNewPassword()));
        userRepository.save(userPO);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        if (userId == null) {
            return;
        }
        if (userId.equals(com.milesight.beaveriot.context.security.SecurityUserContext.getUserId())) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR).detailMessage("delete current user is not allowed").build();
        }
        userRepository.deleteById(userId);
        userRoleRepository.deleteByUserId(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteUsers(BatchDeleteUserRequest batchDeleteUserRequest) {
        List<Long> userIds = batchDeleteUserRequest.getUserIdList();
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        if (userIds.contains(com.milesight.beaveriot.context.security.SecurityUserContext.getUserId())) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR).detailMessage("delete current user is not allowed").build();
        }
        userRepository.deleteAllById(userIds);
        userRoleRepository.deleteByUserIds(userIds);
    }

    public List<UserMenuResponse> getMenusByUserId(Long userId) {
        boolean permissionMode = permissionModule();
        if (!permissionMode) {
            List<MenuPO> menuPOList = menuRepository.findAll();
            return menuPOList.stream().map(menuPO -> {
                UserMenuResponse userMenuResponse = new UserMenuResponse();
                userMenuResponse.setMenuId(menuPO.getId().toString());
                userMenuResponse.setCode(menuPO.getCode());
                userMenuResponse.setName(menuPO.getName());
                userMenuResponse.setType(menuPO.getType());
                userMenuResponse.setParentId(menuPO.getParentId() == null ? null : menuPO.getParentId().toString());
                return userMenuResponse;
            }).toList();
        }
        List<UserRolePO> userRolePOS = userRoleRepository.findAll(filter -> filter.eq(UserRolePO.Fields.userId, userId));
        if (userRolePOS == null || userRolePOS.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> roleIds = userRolePOS.stream().map(UserRolePO::getRoleId).toList();
        List<RolePO> rolePOS = roleRepository.findAll(filter -> filter.in(RolePO.Fields.id, roleIds.toArray()));
        if (rolePOS == null || rolePOS.isEmpty()) {
            return new ArrayList<>();
        }
        boolean hasAllMenu = rolePOS.stream().anyMatch(rolePO -> Objects.equals(rolePO.getName(), UserConstants.SUPER_ADMIN_ROLE_NAME));
        if (hasAllMenu) {
            List<MenuPO> menuPOList = menuRepository.findAll();
            return menuPOList.stream().map(menuPO -> {
                UserMenuResponse userMenuResponse = new UserMenuResponse();
                userMenuResponse.setMenuId(menuPO.getId().toString());
                userMenuResponse.setCode(menuPO.getCode());
                userMenuResponse.setName(menuPO.getName());
                userMenuResponse.setType(menuPO.getType());
                userMenuResponse.setParentId(menuPO.getParentId() == null ? null : menuPO.getParentId().toString());
                return userMenuResponse;
            }).toList();
        }
        List<RoleMenuPO> roleMenuPOS = roleMenuRepository.findAll(filter -> filter.in(RoleMenuPO.Fields.roleId, roleIds.toArray()));
        if (roleMenuPOS == null || roleMenuPOS.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> menuIds = roleMenuPOS.stream().map(RoleMenuPO::getMenuId).toList();
        List<MenuPO> menuPOList = menuRepository.findAll(filterable -> filterable.in(MenuPO.Fields.id, menuIds.toArray()));
        if (menuPOList == null || menuPOList.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, MenuPO> menuMap = menuPOList.stream().collect(Collectors.toMap(MenuPO::getId, Function.identity()));
        return roleMenuPOS.stream().map(roleMenuPO -> {
            if (!menuMap.containsKey(roleMenuPO.getMenuId())) {
                return null;
            }
            UserMenuResponse userMenuResponse = new UserMenuResponse();
            userMenuResponse.setMenuId(roleMenuPO.getMenuId().toString());
            userMenuResponse.setCode(menuMap.get(roleMenuPO.getMenuId()).getCode());
            userMenuResponse.setName(menuMap.get(roleMenuPO.getMenuId()).getName());
            userMenuResponse.setType(menuMap.get(roleMenuPO.getMenuId()).getType());
            userMenuResponse.setParentId(menuMap.get(roleMenuPO.getMenuId()).getParentId() == null ? null : menuMap.get(roleMenuPO.getMenuId()).getParentId().toString());
            return userMenuResponse;
        }).filter(Objects::nonNull).toList();
    }

    public UserPermissionResponse getUserPermission(Long userId, UserPermissionRequest userPermissionRequest) {
        boolean permissionMode = permissionModule();
        if (!permissionMode) {
            UserPermissionResponse userPermissionResponse = new UserPermissionResponse();
            userPermissionResponse.setHasPermission(true);
            return userPermissionResponse;
        }
        UserPermissionResponse userPermissionResponse = new UserPermissionResponse();
        userPermissionResponse.setHasPermission(false);
        List<UserRolePO> userRolePOS = userRoleRepository.findAll(filter -> filter.eq(UserRolePO.Fields.userId, userId));
        if (userRolePOS == null || userRolePOS.isEmpty()) {
            return userPermissionResponse;
        }
        List<Long> roleIds = userRolePOS.stream().map(UserRolePO::getRoleId).toList();
        List<RolePO> rolePOS = roleRepository.findAll(filter -> filter.in(RolePO.Fields.id, roleIds.toArray()));
        if (rolePOS == null || rolePOS.isEmpty()) {
            return userPermissionResponse;
        }
        if (rolePOS.stream().anyMatch(rolePO -> Objects.equals(rolePO.getName(), UserConstants.SUPER_ADMIN_ROLE_NAME))) {
            userPermissionResponse.setHasPermission(true);
            return userPermissionResponse;
        }
        if (userPermissionRequest.getResourceType() == ResourceType.DEVICE) {
            List<DeviceNameDTO> deviceNameDTOList = deviceFacade.getDeviceNameByIds(List.of(Long.parseLong(userPermissionRequest.getResourceId())));
            if (deviceNameDTOList == null || deviceNameDTOList.isEmpty()) {
                return userPermissionResponse;
            }
            String integrationId = deviceNameDTOList.get(0).getIntegrationConfig() == null ? null : deviceNameDTOList.get(0).getIntegrationConfig().getId();
            if (integrationId != null) {
                List<RoleResourcePO> roleResourcePOS = roleResourceRepository.findAll(filter -> filter.in(RoleResourcePO.Fields.roleId, roleIds.toArray())
                        .eq(RoleResourcePO.Fields.resourceType, ResourceType.INTEGRATION)
                        .eq(RoleResourcePO.Fields.resourceId, integrationId));
                if (roleResourcePOS != null && !roleResourcePOS.isEmpty()) {
                    userPermissionResponse.setHasPermission(true);
                    return userPermissionResponse;
                }
            }
        }
        List<RoleResourcePO> roleResourcePOS = roleResourceRepository.findAll(filter -> filter.in(RoleResourcePO.Fields.roleId, roleIds.toArray())
                .eq(RoleResourcePO.Fields.resourceType, userPermissionRequest.getResourceType())
                .eq(RoleResourcePO.Fields.resourceId, userPermissionRequest.getResourceId()));
        if (roleResourcePOS == null || roleResourcePOS.isEmpty()) {
            return userPermissionResponse;
        }
        userPermissionResponse.setHasPermission(true);
        return userPermissionResponse;
    }

    public TenantPO analyzeTenantId(String tenantId) {
        if(!StringUtils.hasText(tenantId)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("tenantId is not exist").build();
        }
        return tenantRepository.findOne(filter -> filter.eq(TenantPO.Fields.id, tenantId)).orElse(null);
    }

    public Long analyzeSuperAdminRoleId() {
        String roleName = UserConstants.SUPER_ADMIN_ROLE_NAME;
        RolePO rolePO = roleRepository.findOne(filter -> filter.eq(RolePO.Fields.name, roleName)).orElseThrow(() -> ServiceException.with(UserErrorCode.ROLE_DOES_NOT_EXIT).detailMessage("role is not exist").build());
        return rolePO.getId();
    }

    public UserPO getUserByEmail(String email, UserStatus status) {
        return userRepository.findOne(filter -> filter.eq(UserPO.Fields.email, email).eq(UserPO.Fields.status, status)).orElse(null);
    }

    public UserPO getUserByEmail(String email) {
        return userRepository.findOne(filter -> filter.eq(UserPO.Fields.email, email)).orElse(null);
    }

    public UserResourceDTO getResource(Long userId, List<ResourceType> resourceTypes) {
        UserResourceDTO userResourceDTO = new UserResourceDTO();
        userResourceDTO.setHasAllResource(false);
        userResourceDTO.setResource(new HashMap<>());
        boolean permissionMode = permissionModule();
        if (!permissionMode) {
            userResourceDTO.setHasAllResource(true);
            return userResourceDTO;
        }
        List<UserRolePO> userRolePOS = userRoleRepository.findAll(filter -> filter.eq(UserRolePO.Fields.userId, userId));
        if (userRolePOS == null || userRolePOS.isEmpty()) {
            return userResourceDTO;
        }
        List<Long> roleIds = userRolePOS.stream().map(UserRolePO::getRoleId).toList();
        List<RolePO> rolePOS = roleRepository.findAll(filter -> filter.in(RolePO.Fields.id, roleIds.toArray()));
        if (rolePOS == null || rolePOS.isEmpty()) {
            return userResourceDTO;
        }
        boolean hasAllResource = rolePOS.stream().anyMatch(rolePO -> Objects.equals(rolePO.getName(), UserConstants.SUPER_ADMIN_ROLE_NAME));
        userResourceDTO.setHasAllResource(hasAllResource);
        if (hasAllResource) {
            return userResourceDTO;
        }
        List<RoleResourcePO> roleResourcePOS = roleResourceRepository.findAll(filter -> filter.in(RoleResourcePO.Fields.roleId, roleIds.toArray()).in(RoleResourcePO.Fields.resourceType, resourceTypes.stream().map(ResourceType::name).toArray()));
        if (roleResourcePOS == null || roleResourcePOS.isEmpty()) {
            return userResourceDTO;
        }
        Map<ResourceType, List<String>> resource = roleResourcePOS.stream().collect(Collectors.groupingBy(RoleResourcePO::getResourceType, Collectors.mapping(RoleResourcePO::getResourceId, Collectors.toList())));
        userResourceDTO.setResource(resource);
        return userResourceDTO;
    }

    public List<UserPO> getUserByIds(List<Long> userIds) {
        return userRepository.findAll(filter -> filter.in(UserPO.Fields.id, userIds.toArray()));
    }

    private boolean permissionModule() {
        try {
            Class.forName("com.milesight.beaveriot.permission.Permission");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

}
