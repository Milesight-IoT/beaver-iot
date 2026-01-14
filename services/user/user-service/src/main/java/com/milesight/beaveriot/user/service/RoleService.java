package com.milesight.beaveriot.user.service;

import com.milesight.beaveriot.base.annotations.cacheable.BatchCacheEvict;
import com.milesight.beaveriot.base.annotations.cacheable.BatchCacheable;
import com.milesight.beaveriot.base.annotations.cacheable.CacheKeys;
import com.milesight.beaveriot.base.enums.ComparisonOperator;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.GenericQueryPageRequest;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.constants.CacheKeyConstants;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.context.support.KeyValidator;
import com.milesight.beaveriot.dashboard.dto.DashboardDTO;
import com.milesight.beaveriot.dashboard.facade.IDashboardFacade;
import com.milesight.beaveriot.data.util.PageConverter;
import com.milesight.beaveriot.device.dto.DeviceNameDTO;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.entity.facade.IEntityFacade;
import com.milesight.beaveriot.user.constants.UserConstants;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.enums.UserErrorCode;
import com.milesight.beaveriot.user.model.Menu;
import com.milesight.beaveriot.user.model.request.CreateRoleRequest;
import com.milesight.beaveriot.user.model.request.RoleMenuRequest;
import com.milesight.beaveriot.user.model.request.RoleResourceListRequest;
import com.milesight.beaveriot.user.model.request.RoleResourceRequest;
import com.milesight.beaveriot.user.model.request.UpdateRoleRequest;
import com.milesight.beaveriot.user.model.request.UserRoleRequest;
import com.milesight.beaveriot.user.model.response.CreateRoleResponse;
import com.milesight.beaveriot.user.model.response.DashboardUndistributedResponse;
import com.milesight.beaveriot.user.model.response.DeviceUndistributedResponse;
import com.milesight.beaveriot.user.model.response.IntegrationUndistributedResponse;
import com.milesight.beaveriot.user.model.response.RoleDashboardResponse;
import com.milesight.beaveriot.user.model.response.RoleDeviceResponse;
import com.milesight.beaveriot.user.model.response.RoleIntegrationResponse;
import com.milesight.beaveriot.user.model.response.RoleMenuResponse;
import com.milesight.beaveriot.user.model.response.RoleResourceResponse;
import com.milesight.beaveriot.user.model.response.RoleResponse;
import com.milesight.beaveriot.user.model.response.UserRoleResponse;
import com.milesight.beaveriot.user.model.response.UserUndistributedResponse;
import com.milesight.beaveriot.user.po.RoleMenuPO;
import com.milesight.beaveriot.user.po.RolePO;
import com.milesight.beaveriot.user.po.RoleResourcePO;
import com.milesight.beaveriot.user.po.UserPO;
import com.milesight.beaveriot.user.po.UserRolePO;
import com.milesight.beaveriot.user.repository.RoleMenuRepository;
import com.milesight.beaveriot.user.repository.RoleRepository;
import com.milesight.beaveriot.user.repository.RoleResourceRepository;
import com.milesight.beaveriot.user.repository.UserRepository;
import com.milesight.beaveriot.user.repository.UserRoleRepository;
import com.milesight.beaveriot.user.util.MenuStore;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.milesight.beaveriot.context.constants.CacheKeyConstants.TENANT_PREFIX;

/**
 * @author loong
 * @date 2024/11/19 17:49
 */
@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RoleResourceRepository roleResourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleMenuRepository roleMenuRepository;

    @Autowired
    private IDashboardFacade dashboardFacade;

    @Autowired
    private IDeviceFacade deviceFacade;

    @Autowired
    private IEntityFacade entityFacade;

    @Autowired
    private IntegrationServiceProvider integrationServiceProvider;

    @Lazy
    @Autowired
    private RoleService self;

    public CreateRoleResponse createRole(CreateRoleRequest createRoleRequest) {
        String name = createRoleRequest.getName();
        if (!StringUtils.hasText(name)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("name is empty").build();
        }

        roleRepository.findOne(filterable -> filterable.eq(RolePO.Fields.name, name))
                .ifPresent(rolePO -> {
                    throw ServiceException.with(UserErrorCode.NAME_REPEATED).detailMessage("name is exist").build();
                });

        RolePO rolePO = new RolePO();
        rolePO.setId(SnowflakeUtil.nextId());
        rolePO.setName(name);
        rolePO.setDescription(createRoleRequest.getDescription());
        roleRepository.save(rolePO);

        CreateRoleResponse createRoleResponse = new CreateRoleResponse();
        createRoleResponse.setRoleId(rolePO.getId().toString());
        return createRoleResponse;
    }

    public void updateRole(Long roleId, UpdateRoleRequest updateRoleRequest) {
        String name = updateRoleRequest.getName();
        if (!StringUtils.hasText(name)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("name is empty").build();
        }

        RolePO otherRolePO = roleRepository.findOne(filterable -> filterable.eq(RolePO.Fields.name, name)).orElse(null);
        if (otherRolePO != null && !Objects.equals(otherRolePO.getId(), roleId)) {
            throw ServiceException.with(UserErrorCode.NAME_REPEATED).detailMessage("name is exist").build();
        }

        RolePO rolePO = roleRepository.findOne(filterable -> filterable.eq(RolePO.Fields.id, roleId))
                .orElseThrow(() -> ServiceException.with(UserErrorCode.ROLE_DOES_NOT_EXIT).detailMessage("role is not exist").build());
        rolePO.setName(name);
        rolePO.setDescription(updateRoleRequest.getDescription());
        roleRepository.save(rolePO);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void deleteRole(Long roleId) {
        RolePO rolePO = roleRepository.findOne(filterable -> filterable.eq(RolePO.Fields.id, roleId)).orElse(null);
        if (rolePO == null) {
            throw ServiceException.with(UserErrorCode.ROLE_DOES_NOT_EXIT).detailMessage("role is not exist").build();
        }

        if (UserConstants.SUPER_ADMIN_ROLE_NAME.equals(rolePO.getName())) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("super admin not allowed to delete").build();
        }

        List<Long> userIds = self.getUserRolePOsByRoleId(roleId).stream()
                .map(UserRolePO::getUserId)
                .toList();
        if (!userIds.isEmpty()) {
            throw ServiceException.with(UserErrorCode.ROLE_STILL_HAS_USER).detailMessage("role has been bound user").build();
        }

        roleRepository.deleteById(roleId);

        self.deleteUserRoleByRoleId(roleId);

        roleMenuRepository.deleteByRoleId(roleId);
        self.evictUserMenusCache(userIds);

        roleResourceRepository.deleteByRoleId(roleId);
        self.evictRoleResourcesCache(List.of(roleId));
    }

    public Page<RoleResponse> getRoles(GenericQueryPageRequest roleListRequest) {
        if (roleListRequest.getSort().getOrders().isEmpty()) {
            roleListRequest.sort(new Sorts().desc(RolePO.Fields.id));
        }

        Page<RolePO> rolePages = roleRepository.findAll(filterable -> filterable.likeIgnoreCase(StringUtils.hasText(roleListRequest.getKeyword()), RolePO.Fields.name, roleListRequest.getKeyword())
                , roleListRequest.toPageable());
        if (rolePages == null || rolePages.getContent().isEmpty()) {
            return Page.empty();
        }

        List<Long> roleIds = rolePages.getContent().stream().map(RolePO::getId).toList();
        List<UserRolePO> userRolePOs = getUserRolePOsByRoleIds(roleIds);
        Map<Long, Long> userRoleCountMap = userRolePOs.stream().collect(Collectors.groupingBy(UserRolePO::getRoleId, Collectors.counting()));

        List<RoleResourcePO> roleIntegrationPOs = self.getRoleResourcePOsByRoleIdsAndResourceTypes(roleIds, Set.of(ResourceType.INTEGRATION));
        Map<Long, Long> roleIntegrationCountMap = roleIntegrationPOs.stream()
                .collect(Collectors.groupingBy(RoleResourcePO::getRoleId, Collectors.counting()));

        return rolePages.map(rolePO -> {
            RoleResponse roleResponse = new RoleResponse();
            roleResponse.setRoleId(rolePO.getId().toString());
            roleResponse.setName(rolePO.getName());
            roleResponse.setCreatedAt(rolePO.getCreatedAt().toString());

            Long userRoleCount = userRoleCountMap.get(rolePO.getId());
            roleResponse.setUserRoleCount(userRoleCount == null ? 0 : userRoleCount.intValue());

            Long roleIntegrationCount = roleIntegrationCountMap.get(rolePO.getId());
            roleResponse.setRoleIntegrationCount(roleIntegrationCount == null ? 0 : roleIntegrationCount.intValue());
            return roleResponse;
        });
    }

    public Page<UserRoleResponse> getUsersByRoleId(Long roleId, GenericQueryPageRequest userRolePageRequest) {
        List<Long> searchUserIds = new ArrayList<>();
        if (StringUtils.hasText(userRolePageRequest.getKeyword())) {
            List<UserPO> userSearchPOs = userRepository.findAll(filterable -> filterable.or(filterable1 -> filterable1.likeIgnoreCase(StringUtils.hasText(userRolePageRequest.getKeyword()), UserPO.Fields.email, userRolePageRequest.getKeyword())
                    .likeIgnoreCase(StringUtils.hasText(userRolePageRequest.getKeyword()), UserPO.Fields.nickname, userRolePageRequest.getKeyword()))
            );
            if (userSearchPOs != null && !userSearchPOs.isEmpty()) {
                searchUserIds.addAll(userSearchPOs.stream().map(UserPO::getId).toList());
            } else {
                return Page.empty();
            }
        }

        Page<UserRolePO> userRolePOs = userRoleRepository.findAll(filterable -> filterable.eq(UserRolePO.Fields.roleId, roleId)
                        .in(!searchUserIds.isEmpty(), UserRolePO.Fields.userId, searchUserIds.toArray())
                , userRolePageRequest.toPageable());
        if (userRolePOs == null || userRolePOs.isEmpty()) {
            return Page.empty();
        }

        List<Long> userIds = userRolePOs.stream().map(UserRolePO::getUserId).toList();
        Map<Long, UserPO> userIdToPO = mapUserIdToPO(userIds);

        return userRolePOs.map(userRolePO -> {
            UserRoleResponse userRoleResponse = new UserRoleResponse();
            userRoleResponse.setRoleId(userRolePO.getRoleId().toString());
            userRoleResponse.setUserId(userRolePO.getUserId().toString());
            UserPO userPO = userIdToPO.get(userRolePO.getUserId());
            userRoleResponse.setUserNickname(userPO == null ? null : userPO.getNickname());
            userRoleResponse.setUserEmail(userPO == null ? null : userPO.getEmail());
            return userRoleResponse;
        });
    }

    public Page<RoleResourceResponse> getResourcesByRoleId(Long roleId, RoleResourceListRequest roleResourceListRequest) {
        ResourceType resourceType = roleResourceListRequest.getResourceType();
        Page<RoleResourcePO> roleResourcePOs = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                        .eq(resourceType != null, RoleResourcePO.Fields.resourceType, resourceType == null ? null : resourceType.name()),
                roleResourceListRequest.toPageable());
        if (roleResourcePOs == null || roleResourcePOs.isEmpty()) {
            return Page.empty();
        }
        return roleResourcePOs.map(roleResourcePO -> {
            RoleResourceResponse roleResourceResponse = new RoleResourceResponse();
            roleResourceResponse.setResourceId(roleResourcePO.getResourceId());
            roleResourceResponse.setResourceType(roleResourcePO.getResourceType());
            return roleResourceResponse;
        });
    }

    public Page<RoleIntegrationResponse> getIntegrationsByRoleId(Long roleId, GenericQueryPageRequest roleIntegrationRequest) {
        List<String> searchIntegrationIds = new ArrayList<>();
        if (StringUtils.hasText(roleIntegrationRequest.getKeyword())) {
            List<Integration> integrations = integrationServiceProvider.findIntegrations(f -> f.getName().toLowerCase().contains(roleIntegrationRequest.getKeyword().toLowerCase()));
            if (integrations != null && !integrations.isEmpty()) {
                List<String> integrationIds = integrations.stream().map(Integration::getId).toList();
                searchIntegrationIds.addAll(integrationIds);
            } else {
                return Page.empty();
            }
        }
        Page<RoleResourcePO> roleResourcePOs = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                        .eq(RoleResourcePO.Fields.resourceType, ResourceType.INTEGRATION.name())
                        .in(!searchIntegrationIds.isEmpty(), RoleResourcePO.Fields.resourceId, searchIntegrationIds.toArray()),
                roleIntegrationRequest.toPageable());
        if (roleResourcePOs == null || roleResourcePOs.isEmpty()) {
            return Page.empty();
        }
        List<String> integrationIds = roleResourcePOs.stream().map(RoleResourcePO::getResourceId).toList();
        List<Integration> integrations = integrationServiceProvider.findIntegrations(f -> integrationIds.contains(f.getId()));
        Map<String, Integration> integrationMap = integrations.stream().collect(Collectors.toMap(Integration::getId, Function.identity()));
        List<DeviceNameDTO> deviceNameDTOList = deviceFacade.getDeviceNameByIntegrations(integrationIds);
        Map<String, List<DeviceNameDTO>> deviceIntegrationMap = deviceNameDTOList.stream().filter(DeviceNameDTO::isIntegrationExists).collect(Collectors.groupingBy(DeviceNameDTO::getIntegrationId));
        Map<String, Long> entityCountMap = entityFacade.countAllEntitiesByIntegrationIds(integrationIds);
        return roleResourcePOs.map(roleResourcePO -> {
            RoleIntegrationResponse roleIntegrationResponse = new RoleIntegrationResponse();
            roleIntegrationResponse.setIntegrationId(roleResourcePO.getResourceId());
            roleIntegrationResponse.setIntegrationName(integrationMap.get(roleResourcePO.getResourceId()) == null ? null : integrationMap.get(roleResourcePO.getResourceId()).getName());
            roleIntegrationResponse.setDeviceNum(deviceIntegrationMap.get(roleResourcePO.getResourceId()) == null ? 0L : deviceIntegrationMap.get(roleResourcePO.getResourceId()).size());
            roleIntegrationResponse.setEntityNum(entityCountMap.get(roleResourcePO.getResourceId()) == null ? 0L : entityCountMap.get(roleResourcePO.getResourceId()));
            return roleIntegrationResponse;
        });
    }

    public Page<RoleDeviceResponse> getDevicesByRoleId(Long roleId, GenericQueryPageRequest roleDeviceRequest) {
        Set<String> searchIntegrationIds = new HashSet<>();
        Set<Long> searchDeviceIds = new HashSet<>();
        boolean isKeywordSearch = StringUtils.hasText(roleDeviceRequest.getKeyword());
        if (isKeywordSearch) {
            List<Integration> integrations = integrationServiceProvider.findIntegrations(f -> f.getName().toLowerCase().contains(roleDeviceRequest.getKeyword().toLowerCase()));
            if (integrations != null && !integrations.isEmpty()) {
                List<String> integrationIds = integrations.stream().map(Integration::getId).toList();
                searchIntegrationIds.addAll(integrationIds);
                List<DeviceNameDTO> integrationDevices = deviceFacade.getDeviceNameByIntegrations(integrationIds);
                if (integrationDevices != null && !integrationDevices.isEmpty()) {
                    searchDeviceIds.addAll(integrationDevices.stream().map(DeviceNameDTO::getId).toList());
                }
            }

            List<Long> deviceIdList = deviceFacade.fuzzySearchDeviceIdsByName(ComparisonOperator.CONTAINS, roleDeviceRequest.getKeyword());
            if (!deviceIdList.isEmpty()) {
                searchDeviceIds.addAll(deviceIdList);
            }

            if (searchDeviceIds.isEmpty() && searchIntegrationIds.isEmpty()) {
                return Page.empty();
            }
        }

        List<RoleResourcePO> roleDevices = new ArrayList<>();
        if (!isKeywordSearch || !searchDeviceIds.isEmpty()) {
            roleDevices = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                    .eq(RoleResourcePO.Fields.resourceType, ResourceType.DEVICE.name())
                    .in(!searchDeviceIds.isEmpty(), RoleResourcePO.Fields.resourceId, searchDeviceIds.toArray()));
        }

        List<Long> responseDeviceIds = new ArrayList<>();
        if (roleDevices != null && !roleDevices.isEmpty()) {
            responseDeviceIds.addAll(roleDevices.stream().map(RoleResourcePO::getResourceId).map(Long::parseLong).toList());
        }

        Set<Long> responseIntegrationDeviceIds = new HashSet<>();
        List<RoleResourcePO> roleIntegrations = roleResourceRepository.findAll(filterable ->
                filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                        .eq(RoleResourcePO.Fields.resourceType, ResourceType.INTEGRATION.name())
                        .in(!searchIntegrationIds.isEmpty(), RoleResourcePO.Fields.resourceId, searchIntegrationIds.toArray()));
        if (!roleIntegrations.isEmpty()) {
            List<String> integrationIds = roleIntegrations.stream().map(RoleResourcePO::getResourceId).toList();
            List<Long> integrationDeviceIds = deviceFacade.getDeviceNameByIntegrations(integrationIds).stream()
                    .filter(d -> !isKeywordSearch
                            || searchDeviceIds.contains(d.getId())
                            || searchIntegrationIds.contains(d.getIntegrationId()))
                    .map(DeviceNameDTO::getId)
                    .toList();
            if (!integrationDeviceIds.isEmpty()) {
                responseIntegrationDeviceIds.addAll(integrationDeviceIds);
                responseDeviceIds.addAll(integrationDeviceIds);
            }
        }

        if (responseDeviceIds.isEmpty()) {
            return Page.empty();
        }

        List<DeviceNameDTO> deviceNameDTOList = deviceFacade.getDeviceNameByIds(responseDeviceIds);
        Map<Long, DeviceNameDTO> deviceMap = deviceNameDTOList.stream().collect(Collectors.toMap(DeviceNameDTO::getId, Function.identity()));
        List<Long> userIds = deviceNameDTOList.stream().map(DeviceNameDTO::getUserId).filter(Objects::nonNull).toList();
        Map<Long, UserPO> userIdToPO = mapUserIdToPO(userIds);

        List<RoleDeviceResponse> roleDeviceResponseList = responseDeviceIds.stream()
                .distinct()
                .map(deviceId -> {
                    RoleDeviceResponse roleDeviceResponse = new RoleDeviceResponse();
                    DeviceNameDTO device = deviceMap.get(deviceId);
                    roleDeviceResponse.setDeviceId(deviceId.toString());
                    roleDeviceResponse.setDeviceName(device == null ? null : device.getName());
                    roleDeviceResponse.setCreatedAt(device == null ? null : device.getCreatedAt().toString());

                    UserPO user = device == null || device.getUserId() == null
                            ? null
                            : userIdToPO.get(device.getUserId());
                    if (user != null) {
                        roleDeviceResponse.setUserId(user.getId().toString());
                        roleDeviceResponse.setUserEmail(user.getEmail());
                        roleDeviceResponse.setUserNickname(user.getNickname());
                    }

                    if (device != null && device.isIntegrationExists()) {
                        roleDeviceResponse.setIntegrationId(device.getIntegrationId());
                        roleDeviceResponse.setIntegrationName(device.getIntegrationName());
                    }
                    roleDeviceResponse.setRoleIntegration(responseIntegrationDeviceIds.contains(deviceId));

                    return roleDeviceResponse;
                })
                .toList();

        return PageConverter.convertToPage(roleDeviceResponseList, roleDeviceRequest.toPageable());
    }

    public Page<RoleDashboardResponse> getDashboardsByRoleId(Long roleId, GenericQueryPageRequest roleDashboardRequest) {
        List<Long> searchDashboardIds = new ArrayList<>();
        if (StringUtils.hasText(roleDashboardRequest.getKeyword())) {
            List<DashboardDTO> dashboardPOs = dashboardFacade.getDashboardsLike(roleDashboardRequest.getKeyword(), Sort.unsorted());
            if (dashboardPOs != null && !dashboardPOs.isEmpty()) {
                searchDashboardIds.addAll(dashboardPOs.stream().map(DashboardDTO::getDashboardId).toList());
            } else {
                return Page.empty();
            }
        }

        Page<RoleResourcePO> roleResourcePOs = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                        .eq(RoleResourcePO.Fields.resourceType, ResourceType.DASHBOARD.name())
                        .in(!searchDashboardIds.isEmpty(), RoleResourcePO.Fields.resourceId, searchDashboardIds.toArray()),
                roleDashboardRequest.toPageable());
        if (roleResourcePOs == null || roleResourcePOs.isEmpty()) {
            return Page.empty();
        }

        List<Long> dashboardIds = roleResourcePOs.stream().map(RoleResourcePO::getResourceId).map(Long::parseLong).distinct().toList();
        List<DashboardDTO> dashboardDTOList = dashboardFacade.getDashboardsByIds(dashboardIds);
        Map<Long, DashboardDTO> dashboardMap = dashboardDTOList.stream().collect(Collectors.toMap(DashboardDTO::getDashboardId, Function.identity()));
        List<Long> userIds = dashboardDTOList.stream().map(DashboardDTO::getUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, UserPO> userMap = mapUserIdToPO(userIds);
        return roleResourcePOs.map(roleResourcePO -> {
            RoleDashboardResponse roleDashboardResponse = new RoleDashboardResponse();
            roleDashboardResponse.setDashboardId(roleResourcePO.getResourceId());
            long resourceId = Long.parseLong(roleResourcePO.getResourceId());
            DashboardDTO dashboardDTO = dashboardMap.get(resourceId);
            if (dashboardDTO != null) {
                roleDashboardResponse.setDashboardName(dashboardDTO.getDashboardName());
                roleDashboardResponse.setCreatedAt(dashboardDTO.getCreatedAt().toString());

                Long userId = dashboardDTO.getUserId();
                UserPO userPO = userId == null ? null : userMap.get(userId);
                if (userPO != null) {
                    roleDashboardResponse.setUserId(userPO.getId().toString());
                    roleDashboardResponse.setUserEmail(userPO.getEmail());
                    roleDashboardResponse.setUserNickname(userPO.getNickname());
                }
            }
            return roleDashboardResponse;
        });
    }

    @NonNull
    private Map<Long, UserPO> mapUserIdToPO(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return userRepository.findAll(filterable -> filterable.in(UserPO.Fields.id, userIds.toArray())).stream()
                .collect(Collectors.toMap(UserPO::getId, Function.identity(), (a, b) -> a));
    }

    public Page<DashboardUndistributedResponse> getUndistributedDashboards(Long roleId, GenericQueryPageRequest dashboardUndistributedRequest) {
        List<DashboardDTO> dashboardDTOList = dashboardFacade.getDashboardsLike(dashboardUndistributedRequest.getKeyword(), dashboardUndistributedRequest.getSort().toSort());
        if (dashboardDTOList == null || dashboardDTOList.isEmpty()) {
            return Page.empty();
        }

        List<RoleResourcePO> roleResourcePOs = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                .eq(RoleResourcePO.Fields.resourceType, ResourceType.DASHBOARD.name()));
        Set<Long> roleDashboardIds = roleResourcePOs.stream()
                .map(RoleResourcePO::getResourceId)
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        List<DashboardDTO> dashboardUndistributedList = dashboardDTOList.stream()
                .filter(dashboardDTO -> !roleDashboardIds.contains(dashboardDTO.getDashboardId()))
                .toList();
        Set<Long> userIds = dashboardUndistributedList.stream()
                .map(DashboardDTO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserPO> userMap = mapUserIdToPO(userIds);

        List<DashboardUndistributedResponse> dashboardUndistributedResponseList = dashboardUndistributedList.stream()
                .map(dashboardDTO -> {
                    DashboardUndistributedResponse dashboardListResponse = new DashboardUndistributedResponse();
                    dashboardListResponse.setDashboardId(dashboardDTO.getDashboardId().toString());
                    dashboardListResponse.setDashboardName(dashboardDTO.getDashboardName());
                    dashboardListResponse.setCreatedAt(dashboardDTO.getCreatedAt().toString());
                    dashboardListResponse.setUserId(dashboardDTO.getUserId() == null ? null : dashboardDTO.getUserId().toString());
                    UserPO userPO = userMap.get(dashboardDTO.getUserId());
                    dashboardListResponse.setUserEmail(userPO == null ? null : userPO.getEmail());
                    dashboardListResponse.setUserNickname(userPO == null ? null : userPO.getNickname());
                    return dashboardListResponse;
                })
                .toList();
        return PageConverter.convertToPage(dashboardUndistributedResponseList, dashboardUndistributedRequest.toPageable());
    }

    public Page<UserUndistributedResponse> getUndistributedUsers(Long roleId, GenericQueryPageRequest userUndistributedRequest) {
        if (userUndistributedRequest.getSort().getOrders().isEmpty()) {
            userUndistributedRequest.sort(new Sorts().desc(UserPO.Fields.createdAt));
        }

        final String keyword = userUndistributedRequest.getKeyword();
        List<UserPO> userPOs = userRepository.findAll(filterable -> filterable.or(filterable1 -> filterable1.likeIgnoreCase(StringUtils.hasText(keyword), UserPO.Fields.nickname, keyword)
                .likeIgnoreCase(StringUtils.hasText(keyword), UserPO.Fields.email, keyword)), userUndistributedRequest.getSort().toSort());
        if (userPOs == null || userPOs.isEmpty()) {
            return Page.empty();
        }

        List<UserRolePO> userRolePOs = userRoleRepository.findAll(filterable -> filterable.eq(UserRolePO.Fields.roleId, roleId));
        Set<Long> userIds = userRolePOs.stream().map(UserRolePO::getUserId).collect(Collectors.toSet());
        List<UserUndistributedResponse> userUndistributedResponseList = userPOs.stream()
                .filter(userPO -> !userIds.contains(userPO.getId()))
                .map(userPO -> {
                    UserUndistributedResponse userUndistributedResponse = new UserUndistributedResponse();
                    userUndistributedResponse.setUserId(userPO.getId().toString());
                    userUndistributedResponse.setEmail(userPO.getEmail());
                    userUndistributedResponse.setNickname(userPO.getNickname());
                    userUndistributedResponse.setCreatedAt(userPO.getCreatedAt().toString());
                    return userUndistributedResponse;
                })
                .toList();
        return PageConverter.convertToPage(userUndistributedResponseList, userUndistributedRequest.toPageable());
    }

    public Page<IntegrationUndistributedResponse> getUndistributedIntegrations(Long roleId, GenericQueryPageRequest integrationUndistributedRequest) {
        List<Integration> integrations = new ArrayList<>();
        if (StringUtils.hasText(integrationUndistributedRequest.getKeyword())) {
            integrations.addAll(integrationServiceProvider.findIntegrations(f -> f.getName().toLowerCase().contains(integrationUndistributedRequest.getKeyword().toLowerCase()) && f.isVisible()));
        } else {
            integrations.addAll(integrationServiceProvider.findVisibleIntegrations());
        }
        if (integrations.isEmpty()) {
            return Page.empty();
        }
        List<RoleResourcePO> roleResourcePOs = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                .eq(RoleResourcePO.Fields.resourceType, ResourceType.INTEGRATION.name()));
        List<String> roleIntegrationIds = roleResourcePOs.stream().map(RoleResourcePO::getResourceId).toList();
        List<IntegrationUndistributedResponse> integrationUndistributedResponseList = integrations.stream().filter(integration -> !roleIntegrationIds.contains(integration.getId())).map(integration -> {
            IntegrationUndistributedResponse integrationUndistributedResponse = new IntegrationUndistributedResponse();
            integrationUndistributedResponse.setIntegrationId(integration.getId());
            integrationUndistributedResponse.setIntegrationName(integration.getName());
            return integrationUndistributedResponse;
        }).toList();
        return PageConverter.convertToPage(integrationUndistributedResponseList, integrationUndistributedRequest.toPageable());
    }

    public Page<DeviceUndistributedResponse> getUndistributedDevices(Long roleId, GenericQueryPageRequest deviceUndistributedRequest) {
        List<Integration> integrations = integrationServiceProvider.findVisibleIntegrations().stream().toList();
        List<String> integrationIds = integrations.stream().map(Integration::getId).toList();
        List<DeviceNameDTO> deviceNameDTOList = deviceFacade.getDeviceNameByIntegrations(integrationIds);
        if (deviceNameDTOList == null || deviceNameDTOList.isEmpty()) {
            return Page.empty();
        }
        if (StringUtils.hasText(deviceUndistributedRequest.getKeyword())) {
            deviceNameDTOList = deviceNameDTOList.stream().filter(deviceNameDTO -> {
                if (deviceNameDTO.getName().toLowerCase().contains(deviceUndistributedRequest.getKeyword().toLowerCase())) {
                    return true;
                }
                String integrationName = deviceNameDTO.getIntegrationName();
                return integrationName != null &&
                        integrationName.toLowerCase().contains(deviceUndistributedRequest.getKeyword().toLowerCase());
            }).toList();
        }
        if (deviceNameDTOList.isEmpty()) {
            return Page.empty();
        }

        List<Long> deviceUserIds = deviceNameDTOList.stream().map(DeviceNameDTO::getUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, UserPO> userMap = mapUserIdToPO(deviceUserIds);
        List<Long> roleDeviceIds = new ArrayList<>();
        List<RoleResourcePO> roleIntegrationPOs = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                .eq(RoleResourcePO.Fields.resourceType, ResourceType.INTEGRATION.name()));
        List<String> roleIntegrationIds = roleIntegrationPOs.stream().map(RoleResourcePO::getResourceId).toList();
        if (!roleIntegrationIds.isEmpty()) {
            List<DeviceNameDTO> deviceNameDTOListByIntegration = deviceFacade.getDeviceNameByIntegrations(roleIntegrationIds);
            roleDeviceIds.addAll(deviceNameDTOListByIntegration.stream().map(DeviceNameDTO::getId).toList());
        }

        List<RoleResourcePO> roleDevicePOs = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                .eq(RoleResourcePO.Fields.resourceType, ResourceType.DEVICE.name()));
        List<Long> roleDeviceIdsByDevice = roleDevicePOs.stream().map(RoleResourcePO::getResourceId).map(Long::parseLong).toList();
        if (!roleDeviceIdsByDevice.isEmpty()) {
            roleDeviceIds.addAll(roleDeviceIdsByDevice);
        }

        List<DeviceUndistributedResponse> deviceUndistributedResponseList = deviceNameDTOList.stream()
                .filter(deviceNameDTO -> !roleDeviceIds.contains(deviceNameDTO.getId()))
                .map(deviceNameDTO -> {
                    DeviceUndistributedResponse deviceUndistributedResponse = new DeviceUndistributedResponse();
                    deviceUndistributedResponse.setDeviceId(deviceNameDTO.getId().toString());
                    deviceUndistributedResponse.setDeviceName(deviceNameDTO.getName());
                    deviceUndistributedResponse.setCreatedAt(deviceNameDTO.getCreatedAt().toString());
                    Long userId = deviceNameDTO.getUserId() == null ? null : deviceNameDTO.getUserId();
                    if (userId != null) {
                        deviceUndistributedResponse.setUserId(userId.toString());
                        deviceUndistributedResponse.setUserEmail(userMap.get(userId) == null ? null : userMap.get(userId).getEmail());
                        deviceUndistributedResponse.setUserNickname(userMap.get(userId) == null ? null : userMap.get(userId).getNickname());
                    }

                    deviceUndistributedResponse.setIntegrationId(deviceNameDTO.getIntegrationId());
                    String integrationName = deviceNameDTO.getIntegrationName();
                    if (integrationName != null) {
                        deviceUndistributedResponse.setIntegrationName(integrationName);
                    }
                    return deviceUndistributedResponse;
                })
                .toList();
        return PageConverter.convertToPage(deviceUndistributedResponseList, deviceUndistributedRequest.toPageable());
    }

    @Transactional(rollbackFor = Throwable.class)
    public void associateUser(Long roleId, UserRoleRequest userRoleRequest) {
        List<Long> userIds = userRoleRequest.getUserIds();
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        List<UserRolePO> userRolePOs = userRepository.findAll(filterable -> filterable.in(UserPO.Fields.id, userIds.toArray()))
                .stream()
                .map(UserPO::getId)
                .map(userId -> buildUserRolePO(userId, roleId))
                .toList();
        userRoleRepository.saveAll(userRolePOs);

        self.evictRoleUsersCache(List.of(roleId));
        self.evictUserRolesCache(userIds);
        self.evictUserMenusCache(userIds);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void disassociateUser(Long roleId, UserRoleRequest userRoleRequest) {
        if (CollectionUtils.isEmpty(userRoleRequest.getUserIds())) {
            return;
        }
        Set<Long> userIds = new HashSet<>(userRoleRequest.getUserIds());

        List<UserRolePO> userRolePOs = self.getUserRolePOsByRoleId(roleId).stream()
                .filter(userRolePO -> userIds.contains(userRolePO.getUserId()))
                .toList();
        if (CollectionUtils.isEmpty(userRolePOs)) {
            return;
        }
        userRoleRepository.deleteAll(userRolePOs);
        self.evictRoleUsersCache(List.of(roleId));
        self.evictUserRolesCache(userIds);
        self.evictUserMenusCache(userIds);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void associateResource(Long roleId, RoleResourceRequest roleResourceRequest) {
        List<RoleResourceRequest.Resource> resources = roleResourceRequest.getResources();
        if (resources == null || resources.isEmpty()) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("resources is empty").build();
        }

        List<RoleResourcePO> roleResourcePOs = resources.stream()
                .map(resource -> {
                    RoleResourcePO roleResourcePO = new RoleResourcePO();
                    roleResourcePO.setId(SnowflakeUtil.nextId());
                    roleResourcePO.setRoleId(roleId);
                    KeyValidator.validate(resource.getId());
                    roleResourcePO.setResourceId(resource.getId());
                    roleResourcePO.setResourceType(resource.getType());
                    return roleResourcePO;
                }).toList();
        roleResourceRepository.saveAll(roleResourcePOs);

        self.evictRoleResourcesCache(List.of(roleId));
    }

    public void associateResource(Long userId, ResourceType resourceType, List<Long> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return;
        }

        List<UserRolePO> userRolePOList = userRoleRepository.findAll(filterable -> filterable.eq(UserRolePO.Fields.userId, userId));
        if (userRolePOList == null || userRolePOList.isEmpty()) {
            return;
        }

        List<RoleResourcePO> roleResourcePOs = new ArrayList<>();
        userRolePOList.forEach(userRolePO -> {
            Long roleId = userRolePO.getRoleId();
            resourceIds.forEach(resourceId -> {
                RoleResourcePO roleResourcePO = new RoleResourcePO();
                roleResourcePO.setId(SnowflakeUtil.nextId());
                roleResourcePO.setRoleId(roleId);
                roleResourcePO.setResourceId(resourceId.toString());
                roleResourcePO.setResourceType(resourceType);
                roleResourcePOs.add(roleResourcePO);
            });
        });
        roleResourceRepository.saveAll(roleResourcePOs);

        Set<Long> roleIds = roleResourcePOs.stream().map(RoleResourcePO::getRoleId).collect(Collectors.toSet());
        self.evictRoleResourcesCache(roleIds);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void disassociateResource(Long roleId, RoleResourceRequest roleResourceRequest) {
        List<RoleResourceRequest.Resource> resources = roleResourceRequest.getResources();
        if (resources == null || resources.isEmpty()) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("resources is empty").build();
        }

        Map<String, RoleResourceRequest.Resource> keyToResource = resources.stream()
                .collect(Collectors.toMap(v -> getResourceKey(v.getType(), v.getId()), Function.identity(), (a, b) -> a));
        List<RoleResourcePO> roleResourcePOs = getRoleResourcePOsByRoleId(roleId);
        List<RoleResourcePO> matchedResources = roleResourcePOs
                .stream()
                .filter(r -> keyToResource.containsKey(getResourceKey(r.getResourceType(), r.getResourceId())))
                .toList();

        if (!matchedResources.isEmpty()) {
            roleResourceRepository.deleteAll(matchedResources);
        }
        self.evictRoleResourcesCache(List.of(roleId));
    }

    public void deleteResource(ResourceType resourceType, List<Long> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return;
        }

        List<RoleResourcePO> roleResourcePOs = roleResourceRepository.findAll(filterable -> filterable.in(RoleResourcePO.Fields.resourceId, resourceIds.toArray()).eq(RoleResourcePO.Fields.resourceType, resourceType));
        if (roleResourcePOs != null && !roleResourcePOs.isEmpty()) {
            Set<Long> roleIds = roleResourcePOs.stream().map(RoleResourcePO::getRoleId).collect(Collectors.toSet());
            self.evictRoleResourcesCache(roleIds);

            roleResourceRepository.deleteAll(roleResourcePOs);
        }
    }

    @NonNull
    private static String getResourceKey(ResourceType resourceType, String resourceId) {
        return resourceType.name() + ":" + resourceId;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void associateMenu(Long roleId, RoleMenuRequest roleMenuRequest) {
        roleMenuRepository.deleteByRoleId(roleId);
        List<Long> userIds = getUserRolePOsByRoleId(roleId).stream()
                .map(UserRolePO::getUserId)
                .toList();
        self.evictUserMenusCache(userIds);

        List<Long> requestMenuIds = roleMenuRequest.getMenuIds();
        if (requestMenuIds == null || requestMenuIds.isEmpty()) {
            return;
        }

        Set<Long> menuIds = new HashSet<>(requestMenuIds);
        List<RoleMenuPO> roleMenuPOs = MenuStore.getAllMenus()
                .stream()
                .map(Menu::getId)
                .filter(menuIds::contains)
                .map(menuId -> {
                    RoleMenuPO roleMenuPO = new RoleMenuPO();
                    roleMenuPO.setId(SnowflakeUtil.nextId());
                    roleMenuPO.setRoleId(roleId);
                    roleMenuPO.setMenuId(menuId);
                    return roleMenuPO;
                }).toList();

        if (!roleMenuPOs.isEmpty()) {
            roleMenuRepository.saveAll(roleMenuPOs);
        }
    }

    @BatchCacheEvict(cacheNames = CacheKeyConstants.USER_ID_TO_MENUS, keyPrefix = TENANT_PREFIX)
    public void evictUserMenusCache(@CacheKeys Collection<Long> userIds) {
        // do nothing
    }


    public List<RolePO> getRoleByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleRepository.findAll(filter -> filter.in(RolePO.Fields.id, roleIds.toArray()));
    }

    public List<RoleMenuResponse> getMenusByRoleId(Long roleId) {
        List<RoleMenuPO> roleMenuPOs = getRoleMenuPOsByRoleId(roleId);
        if (roleMenuPOs == null || roleMenuPOs.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> menuIds = roleMenuPOs.stream().map(RoleMenuPO::getMenuId).collect(Collectors.toSet());
        Map<Long, Menu> menuMap = MenuStore.getAllMenus().stream()
                .filter(menuPO -> menuIds.contains(menuPO.getId()))
                .collect(Collectors.toMap(Menu::getId, Function.identity(), (a, b) -> a));

        return roleMenuPOs.stream()
                .map(roleMenuPO -> {
                    Menu menu = menuMap.get(roleMenuPO.getMenuId());
                    if (menu == null) {
                        return null;
                    }

                    RoleMenuResponse roleMenuResponse = new RoleMenuResponse();
                    roleMenuResponse.setMenuId(roleMenuPO.getMenuId().toString());
                    roleMenuResponse.setCode(menu.getCode());
                    roleMenuResponse.setName(menu.getName());
                    roleMenuResponse.setType(menu.getType());
                    roleMenuResponse.setParentId(menu.getParentId() == null ? null : menu.getParentId().toString());
                    return roleMenuResponse;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public List<RoleMenuPO> getRoleMenuPOsByRoleId(Long roleId) {
        return self.getRoleMenuPOsByRoleIds(List.of(roleId));
    }

    public List<RoleMenuPO> getRoleMenuPOsByRoleIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMenuRepository.findAll(filter -> filter.in(RoleMenuPO.Fields.roleId, roleIds.toArray()));
    }

    @NonNull
    private static UserRolePO buildUserRolePO(Long userId, Long roleId) {
        UserRolePO userRolePO = new UserRolePO();
        userRolePO.setId(SnowflakeUtil.nextId());
        userRolePO.setRoleId(roleId);
        userRolePO.setUserId(userId);
        return userRolePO;
    }

    @Transactional(rollbackFor = Throwable.class)
    public UserRolePO createUserRole(Long userId, Long roleId) {
        return userRoleRepository.save(buildUserRolePO(userId, roleId));
    }

    public List<UserRolePO> getUserRolePOsByRoleId(Long roleId) {
        return getUserRolePOsByRoleIds(List.of(roleId));
    }


    public List<UserRolePO> getUserRolePOsByRoleIds(Collection<Long> roleIds) {
        return self.mapRoleIdToUserRolePOs(roleIds)
                .values()
                .stream()
                .flatMap(Collection::stream)
                .toList();
    }

    @BatchCacheable(cacheNames = CacheKeyConstants.ROLE_ID_TO_USERS, keyPrefix = TENANT_PREFIX)
    public Map<Long, List<UserRolePO>> mapRoleIdToUserRolePOs(@CacheKeys Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRoleRepository.findAll(filterable -> filterable.in(UserRolePO.Fields.roleId, roleIds.toArray()))
                .stream()
                .collect(Collectors.groupingBy(UserRolePO::getRoleId));
    }

    public List<UserRolePO> getUserRolePOsByUserId(Long userId) {
        return getUserRolePOsByUserIds(List.of(userId));
    }

    public List<UserRolePO> getUserRolePOsByUserIds(Collection<Long> userIds) {
        return self.mapUserIdToUserRolePOs(userIds)
                .values()
                .stream()
                .flatMap(Collection::stream)
                .toList();
    }

    @BatchCacheable(cacheNames = CacheKeyConstants.USER_ID_TO_ROLES, keyPrefix = TENANT_PREFIX)
    public Map<Long, List<UserRolePO>> mapUserIdToUserRolePOs(@CacheKeys Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRoleRepository.findAll(filterable -> filterable.in(UserRolePO.Fields.userId, userIds.toArray()))
                .stream()
                .collect(Collectors.groupingBy(UserRolePO::getUserId));
    }

    @BatchCacheEvict(cacheNames = CacheKeyConstants.USER_ID_TO_ROLES, keyPrefix = TENANT_PREFIX)
    public void evictUserRolesCache(@CacheKeys Collection<Long> userIds) {
        // do nothing
    }

    @BatchCacheEvict(cacheNames = CacheKeyConstants.ROLE_ID_TO_USERS, keyPrefix = TENANT_PREFIX)
    public void evictRoleUsersCache(@CacheKeys Collection<Long> roleIds) {
        // do nothing
    }

    @Transactional(rollbackFor = Throwable.class)
    public void deleteUserRoleByUserId(Long userId) {
        self.deleteUserRoleByUserIds(List.of(userId));
    }

    @Transactional(rollbackFor = Throwable.class)
    public void deleteUserRoleByUserIds(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }

        List<Long> roleIds = userRoleRepository.findAll(filterable -> filterable.in(UserRolePO.Fields.userId, userIds.toArray()))
                .stream()
                .map(UserRolePO::getRoleId)
                .toList();
        self.evictRoleUsersCache(roleIds);

        userRoleRepository.deleteByUserIds(userIds);
        self.evictUserRolesCache(userIds);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void deleteUserRoleByRoleId(Long roleId) {
        userRoleRepository.deleteByRoleId(roleId);
        self.evictUserRolesCacheByRoleId(roleId);
    }

    @CacheEvict(cacheNames = CacheKeyConstants.ROLE_ID_TO_USERS, key = "T(com.milesight.beaveriot.context.security.TenantContext).getTenantId()+':'+#p0")
    public void evictUserRolesCacheByRoleId(Long roleId) {
        // do nothing
    }

    public List<RoleResourcePO> getRoleResourcePOsByRoleId(Long roleId) {
        return getRoleResourcePOsByRoleIds(List.of(roleId));
    }

    public List<RoleResourcePO> getRoleResourcePOsByRoleIdsAndResourceTypeAndResourceId(Collection<Long> roleIds, ResourceType resourceType, String resourceId) {
        List<RoleResourcePO> roleResourcePOs = getRoleResourcePOsByRoleIds(roleIds);
        return roleResourcePOs.stream()
                .filter(roleResourcePO -> Objects.equals(resourceType, roleResourcePO.getResourceType())
                        && Objects.equals(resourceId, roleResourcePO.getResourceId()))
                .toList();
    }

    public List<RoleResourcePO> getRoleResourcePOsByRoleIdsAndResourceTypes(List<Long> roleIds, Collection<ResourceType> resourceTypes) {
        List<RoleResourcePO> roleResourcePOs = getRoleResourcePOsByRoleIds(roleIds);
        return roleResourcePOs.stream()
                .filter(roleResourcePO -> resourceTypes.contains(roleResourcePO.getResourceType()))
                .toList();
    }

    public List<RoleResourcePO> getRoleResourcePOsByRoleIds(Collection<Long> roleIds) {
        return self.mapRoleIdToRoleResourcePOs(roleIds)
                .values()
                .stream()
                .flatMap(Collection::stream)
                .toList();
    }

    @BatchCacheable(cacheNames = CacheKeyConstants.ROLE_ID_TO_RESOURCES, keyPrefix = TENANT_PREFIX)
    public Map<Long, List<RoleResourcePO>> mapRoleIdToRoleResourcePOs(@CacheKeys Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return roleResourceRepository.findAll(filter -> filter.in(RoleResourcePO.Fields.roleId, roleIds.toArray()))
                .stream()
                .collect(Collectors.groupingBy(RoleResourcePO::getRoleId));
    }

    @BatchCacheEvict(cacheNames = CacheKeyConstants.ROLE_ID_TO_RESOURCES, keyPrefix = TENANT_PREFIX)
    public void evictRoleResourcesCache(@CacheKeys Collection<Long> roleIds) {
        // do nothing
    }

    public Long getSuperAdminRoleId() {
        String roleName = UserConstants.SUPER_ADMIN_ROLE_NAME;
        RolePO rolePO = roleRepository.findOne(filter -> filter.eq(RolePO.Fields.name, roleName)).orElseThrow(() -> ServiceException.with(UserErrorCode.ROLE_DOES_NOT_EXIT).detailMessage("role is not exist").build());
        return rolePO.getId();
    }

}
