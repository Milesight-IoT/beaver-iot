package com.milesight.beaveriot.user.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.GenericQueryPageRequest;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.aspect.WithSecurityUserContext;
import com.milesight.beaveriot.context.security.SecurityUser;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.device.dto.DeviceNameDTO;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.user.constants.UserConstants;
import com.milesight.beaveriot.user.dto.UserResourceDTO;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.enums.UserErrorCode;
import com.milesight.beaveriot.user.enums.UserStatus;
import com.milesight.beaveriot.user.model.Menu;
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
import com.milesight.beaveriot.user.po.RoleMenuPO;
import com.milesight.beaveriot.user.po.RolePO;
import com.milesight.beaveriot.user.po.RoleResourcePO;
import com.milesight.beaveriot.user.po.TenantPO;
import com.milesight.beaveriot.user.po.UserPO;
import com.milesight.beaveriot.user.po.UserRolePO;
import com.milesight.beaveriot.user.repository.TenantRepository;
import com.milesight.beaveriot.user.repository.UserRepository;
import com.milesight.beaveriot.user.util.MenuStore;
import com.milesight.beaveriot.user.util.SignUtils;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author loong
 * @date 2024/10/14 8:42
 */
@Slf4j
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Lazy
    @Autowired
    private IDeviceFacade deviceFacade;

    @Lazy
    @Autowired
    private RoleService roleService;

    @WithSecurityUserContext(tenantId = "#tenantId")
    @Transactional(rollbackFor = Exception.class)
    public void register(String tenantId, UserRegisterRequest userRegisterRequest) {
        validateTenantHasUsers(tenantId);

        String email = userRegisterRequest.getEmail();
        String nickname = userRegisterRequest.getNickname();
        String password = userRegisterRequest.getPassword();
        log.debug("register user: tenantId={}, email={}, nickname={}", tenantId, email, nickname);

        validateEmailPasswordAndEnsureNotExist(email, nickname, password);
        UserPO userPO = createUser(email, nickname, password);
        roleService.createUserRole(userPO.getId(), roleService.getSuperAdminRoleId());
    }

    private void validateTenantHasUsers(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("tenantId is not exist").build();
        }

        Long userCount = userRepository.count();
        if (userCount > 0) {
            throw ServiceException.with(UserErrorCode.TENANT_USER_INITED).build();
        }
    }

    private void validateEmailPasswordAndEnsureNotExist(String email, String nickname, String password) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(nickname) || !StringUtils.hasText(password)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("email and nickname and password must be not null").build();
        }
        UserPO userPO = getUserByEmail(email);
        if (userPO != null) {
            throw ServiceException.with(UserErrorCode.USER_REGISTER_EMAIL_EXIST).build();
        }
    }

    @NonNull
    private UserPO createUser(String email, String nickname, String password) {
        UserPO userPO = new UserPO();
        userPO.setId(SnowflakeUtil.nextId());
        userPO.setEmail(email);
        userPO.setEmailHash(SignUtils.sha256Hex(email));
        userPO.setNickname(nickname);
        userPO.setPassword(new BCryptPasswordEncoder().encode(password));
        userPO.setPreference(null);
        userPO.setStatus(UserStatus.ENABLE);
        userRepository.save(userPO);
        return userPO;
    }

    @WithSecurityUserContext(tenantId = "#tenantId")
    public UserStatusResponse status(String tenantId) {
        List<UserPO> users = userRepository.findAll();
        boolean isInit = users != null && !users.isEmpty();
        UserStatusResponse userStatusResponse = new UserStatusResponse();
        userStatusResponse.setInit(isInit);
        return userStatusResponse;
    }

    public UserInfoResponse getUserInfo() {
        SecurityUser securityUser = SecurityUserContext.getSecurityUser();
        UserInfoResponse userInfoResponse = new UserInfoResponse();
        if (securityUser == null) {
            return userInfoResponse;
        }

        Long userId = securityUser.getUserId();
        userInfoResponse.setUserId(userId.toString());
        userInfoResponse.setTenantId(securityUser.getTenantId());
        userInfoResponse.setNickname(securityUser.getNickname());
        userInfoResponse.setEmail(securityUser.getEmail());
        userInfoResponse.setCreatedAt(securityUser.getCreatedAt());

        List<Menu> menus = MenuStore.getAllMenus();
        if (!menus.isEmpty()) {
            List<UserMenuResponse> userMenuResponses = getMenusByUserId(userId);
            List<String> userMenuParentIds = userMenuResponses.stream().map(UserMenuResponse::getParentId).filter(Objects::nonNull).distinct().toList();
            List<UserMenuResponse> allUserMenuResponses = new ArrayList<>(userMenuResponses);
            recurrenceParentMenu(menus, userMenuParentIds, allUserMenuResponses);

            List<MenuResponse> menuResponses = allUserMenuResponses.stream()
                    .filter(userMenuResponse -> userMenuResponse.getParentId() == null)
                    .distinct()
                    .map(userMenuResponse -> buildMenuResponse(allUserMenuResponses, userMenuResponse))
                    .toList();
            userInfoResponse.setMenus(menuResponses);
        }

        AtomicBoolean isSuperAdmin = new AtomicBoolean(false);
        List<UserRolePO> userRolePOs = roleService.getUserRolePOsByUserId(userId);
        if (userRolePOs != null && !userRolePOs.isEmpty()) {
            List<Long> roleIds = userRolePOs.stream().map(UserRolePO::getRoleId).toList();
            List<RolePO> rolePOs = roleService.getRoleByRoleIds(roleIds);
            List<UserInfoResponse.Role> roles = rolePOs.stream()
                    .map(rolePO -> {
                        if (rolePO.getName().equals(UserConstants.SUPER_ADMIN_ROLE_NAME)) {
                            isSuperAdmin.set(true);
                        }
                        UserInfoResponse.Role role = new UserInfoResponse.Role();
                        role.setRoleId(rolePO.getId().toString());
                        role.setRoleName(rolePO.getName());
                        return role;
                    })
                    .toList();
            userInfoResponse.setRoles(roles);
        }
        userInfoResponse.setIsSuperAdmin(isSuperAdmin.get());
        return userInfoResponse;
    }

    @NonNull
    private MenuResponse buildMenuResponse(List<UserMenuResponse> allUserMenuResponses, UserMenuResponse userMenuResponse) {
        MenuResponse menuResponse = new MenuResponse();
        menuResponse.setMenuId(userMenuResponse.getMenuId());
        menuResponse.setCode(userMenuResponse.getCode());
        menuResponse.setName(userMenuResponse.getName());
        menuResponse.setType(userMenuResponse.getType());
        menuResponse.setParentId(userMenuResponse.getParentId());

        List<MenuResponse> menuChild = recurrenceChildMenu(allUserMenuResponses, userMenuResponse.getMenuId());
        menuResponse.setChildren(menuChild);
        return menuResponse;
    }

    private void recurrenceParentMenu(List<Menu> menus, List<String> userMenuParentIds, List<UserMenuResponse> allUserMenuResponses) {
        if (userMenuParentIds == null || userMenuParentIds.isEmpty()) {
            return;
        }

        List<Menu> parentMenus = menus.stream().filter(t -> userMenuParentIds.contains(t.getId().toString())).toList();
        if (parentMenus.isEmpty()) {
            return;
        }

        List<UserMenuResponse> parentUserMenuResponses = parentMenus.stream()
                .map(UserService::buildUserMenuResponse)
                .toList();
        allUserMenuResponses.addAll(parentUserMenuResponses);

        List<String> parentMenuParentIds = parentMenus.stream()
                .map(Menu::getParentId)
                .filter(Objects::nonNull)
                .map(Objects::toString)
                .distinct()
                .toList();
        recurrenceParentMenu(menus, parentMenuParentIds, allUserMenuResponses);
    }

    private List<MenuResponse> recurrenceChildMenu(List<UserMenuResponse> menuResponses, String parentId) {
        return menuResponses.stream()
                .filter(t -> t.getParentId() != null && t.getParentId().equals(parentId))
                .distinct()
                .map(t -> buildMenuResponse(menuResponses, t))
                .toList();
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
        List<UserRolePO> userRolePOs = roleService.getUserRolePOsByUserIds(userIds);
        if (userRolePOs != null && !userRolePOs.isEmpty()) {
            userRoleIdMap.putAll(userRolePOs.stream().collect(Collectors.groupingBy(UserRolePO::getUserId, Collectors.mapping(UserRolePO::getRoleId, Collectors.toList()))));
            List<Long> roleIds = userRolePOs.stream().map(UserRolePO::getRoleId).toList();
            List<RolePO> rolePOs = roleService.getRoleByRoleIds(roleIds);
            if (rolePOs != null && !rolePOs.isEmpty()) {
                roleNameMap.putAll(rolePOs.stream().collect(Collectors.toMap(RolePO::getId, RolePO::getName)));
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
        validateEmailPasswordAndEnsureNotExist(email, nickname, password);
        createUser(email, nickname, password);
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
        UserPO userPO = userRepository.findOne(filter -> filter.eq(UserPO.Fields.id, SecurityUserContext.getUserId())).orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).build());
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
        if (userId.equals(SecurityUserContext.getUserId())) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR).detailMessage("delete current user is not allowed").build();
        }
        userRepository.deleteById(userId);
        roleService.deleteUserRoleByUserId(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteUsers(BatchDeleteUserRequest batchDeleteUserRequest) {
        List<Long> userIds = batchDeleteUserRequest.getUserIdList();
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        if (userIds.contains(SecurityUserContext.getUserId())) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR).detailMessage("delete current user is not allowed").build();
        }
        userRepository.deleteAllById(userIds);
        roleService.deleteUserRoleByUserIds(userIds);
    }

    public List<UserMenuResponse> getMenusByUserId(Long userId) {
        boolean permissionMode = permissionModule();
        if (!permissionMode) {
            return getAllUserMenuResponses();
        }
        List<UserRolePO> userRolePOs = roleService.getUserRolePOsByUserId(userId);
        if (userRolePOs == null || userRolePOs.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> roleIds = userRolePOs.stream().map(UserRolePO::getRoleId).toList();
        List<RolePO> rolePOs = roleService.getRoleByRoleIds(roleIds);
        if (rolePOs == null || rolePOs.isEmpty()) {
            return new ArrayList<>();
        }
        boolean hasAllMenu = rolePOs.stream().anyMatch(rolePO -> Objects.equals(rolePO.getName(), UserConstants.SUPER_ADMIN_ROLE_NAME));
        if (hasAllMenu) {
            return getAllUserMenuResponses();
        }

        List<RoleMenuPO> roleMenuPOs = roleService.getRoleMenuPOsByRoleIds(roleIds);
        if (roleMenuPOs == null || roleMenuPOs.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> menuIds = roleMenuPOs.stream().map(RoleMenuPO::getMenuId).collect(Collectors.toSet());
        Map<Long, Menu> idToMenu = MenuStore.getAllMenus().stream()
                .filter(menuPO -> menuIds.contains(menuPO.getId()))
                .collect(Collectors.toMap(Menu::getId, Function.identity()));
        if (idToMenu.isEmpty()) {
            return new ArrayList<>();
        }

        return roleMenuPOs.stream().map(roleMenuPO -> {
                    Menu menu = idToMenu.get(roleMenuPO.getMenuId());
                    if (menu == null) {
                        return null;
                    }

                    UserMenuResponse userMenuResponse = new UserMenuResponse();
                    userMenuResponse.setMenuId(roleMenuPO.getMenuId().toString());
                    userMenuResponse.setCode(menu.getCode());
                    userMenuResponse.setName(menu.getName());
                    userMenuResponse.setType(menu.getType());
                    userMenuResponse.setParentId(menu.getParentId() == null ? null : menu.getParentId().toString());
                    return userMenuResponse;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @NonNull
    private List<UserMenuResponse> getAllUserMenuResponses() {
        return MenuStore.getAllMenus().stream()
                .map(UserService::buildUserMenuResponse)
                .toList();
    }

    @NonNull
    private static UserMenuResponse buildUserMenuResponse(Menu menu) {
        UserMenuResponse userMenuResponse = new UserMenuResponse();
        userMenuResponse.setMenuId(menu.getId().toString());
        userMenuResponse.setCode(menu.getCode());
        userMenuResponse.setName(menu.getName());
        userMenuResponse.setType(menu.getType());
        userMenuResponse.setParentId(menu.getParentId() == null ? null : menu.getParentId().toString());
        return userMenuResponse;
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

        List<Long> roleIds = roleService.getUserRolePOsByUserId(userId).stream()
                .map(UserRolePO::getRoleId)
                .toList();

        Boolean isSuperAdmin = containsSuperAdmin(roleIds).orElse(null);
        if (!Boolean.FALSE.equals(isSuperAdmin)) {
            userPermissionResponse.setHasPermission(Boolean.TRUE.equals(isSuperAdmin));
            return userPermissionResponse;
        }

        String resourceId = userPermissionRequest.getResourceId();
        ResourceType resourceType = userPermissionRequest.getResourceType();
        if (resourceType == ResourceType.DEVICE) {
            List<DeviceNameDTO> deviceNameDTOList = deviceFacade.getDeviceNameByIds(List.of(Long.parseLong(resourceId)));
            if (deviceNameDTOList == null || deviceNameDTOList.isEmpty()) {
                return userPermissionResponse;
            }
            if (deviceNameDTOList.get(0).isIntegrationExists()) {
                List<RoleResourcePO> roleResourcePOs = roleService.getRoleResourcePOsByRoleIdsAndResourceTypeAndResourceId(roleIds, ResourceType.INTEGRATION, deviceNameDTOList.get(0).getIntegrationId());
                if (roleResourcePOs != null && !roleResourcePOs.isEmpty()) {
                    userPermissionResponse.setHasPermission(true);
                    return userPermissionResponse;
                }
            }
        }

        List<RoleResourcePO> roleResourcePOs = roleService.getRoleResourcePOsByRoleIdsAndResourceTypeAndResourceId(
                roleIds, resourceType, resourceId);
        if (roleResourcePOs == null || roleResourcePOs.isEmpty()) {
            return userPermissionResponse;
        }

        userPermissionResponse.setHasPermission(true);
        return userPermissionResponse;
    }

    public TenantPO analyzeTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("tenantId is not exist").build();
        }
        return tenantRepository.findOne(filter -> filter.eq(TenantPO.Fields.id, tenantId)).orElse(null);
    }

    public UserPO getUserByEmail(String email, UserStatus status) {
        return userRepository.findOne(filter -> filter.eq(UserPO.Fields.email, email).eq(UserPO.Fields.status, status)).orElse(null);
    }

    public UserPO getUserByEmail(String email) {
        return userRepository.findOne(filter -> filter.eq(UserPO.Fields.email, email)).orElse(null);
    }

    public UserResourceDTO getUserResource(Long userId, List<ResourceType> resourceTypes) {
        UserResourceDTO userResourceDTO = new UserResourceDTO();
        userResourceDTO.setHasAllResource(false);
        userResourceDTO.setResource(Collections.emptyMap());
        boolean permissionMode = permissionModule();
        if (!permissionMode) {
            userResourceDTO.setHasAllResource(true);
            return userResourceDTO;
        }

        List<Long> roleIds = roleService.getUserRolePOsByUserId(userId).stream()
                .map(UserRolePO::getRoleId)
                .toList();

        Boolean isSuperAdmin = containsSuperAdmin(roleIds).orElse(null);
        if (!Boolean.FALSE.equals(isSuperAdmin)) {
            userResourceDTO.setHasAllResource(Boolean.TRUE.equals(isSuperAdmin));
            return userResourceDTO;
        }

        List<RoleResourcePO> roleResourcePOs = roleService.getRoleResourcePOsByRoleIdsAndResourceTypes(roleIds, resourceTypes);
        if (roleResourcePOs == null || roleResourcePOs.isEmpty()) {
            return userResourceDTO;
        }

        Map<ResourceType, List<String>> resource = roleResourcePOs.stream()
                .collect(Collectors.groupingBy(RoleResourcePO::getResourceType, Collectors.mapping(RoleResourcePO::getResourceId, Collectors.toList())));
        userResourceDTO.setResource(resource);
        return userResourceDTO;
    }

    private Optional<Boolean> containsSuperAdmin(List<Long> roleIds) {
        List<RolePO> rolePOs = roleService.getRoleByRoleIds(roleIds);
        if (CollectionUtils.isEmpty(rolePOs)) {
            return Optional.empty();
        }
        boolean isSuperAdmin = rolePOs.stream()
                .anyMatch(rolePO -> UserConstants.SUPER_ADMIN_ROLE_NAME.equals(rolePO.getName()));
        return Optional.of(isSuperAdmin);
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
