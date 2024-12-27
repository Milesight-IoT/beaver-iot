package com.milesight.beaveriot.user.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.dashboard.dto.DashboardDTO;
import com.milesight.beaveriot.dashboard.facade.IDashboardFacade;
import com.milesight.beaveriot.device.dto.DeviceNameDTO;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.user.constants.UserConstants;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.model.request.CreateRoleRequest;
import com.milesight.beaveriot.user.model.request.DashboardUndistributedRequest;
import com.milesight.beaveriot.user.model.request.DeviceUndistributedRequest;
import com.milesight.beaveriot.user.model.request.IntegrationUndistributedRequest;
import com.milesight.beaveriot.user.model.request.RoleDashboardRequest;
import com.milesight.beaveriot.user.model.request.RoleDeviceRequest;
import com.milesight.beaveriot.user.model.request.RoleIntegrationRequest;
import com.milesight.beaveriot.user.model.request.RoleListRequest;
import com.milesight.beaveriot.user.model.request.RoleMenuRequest;
import com.milesight.beaveriot.user.model.request.RoleResourceListRequest;
import com.milesight.beaveriot.user.model.request.RoleResourceRequest;
import com.milesight.beaveriot.user.model.request.UpdateRoleRequest;
import com.milesight.beaveriot.user.model.request.UserRolePageRequest;
import com.milesight.beaveriot.user.model.request.UserRoleRequest;
import com.milesight.beaveriot.user.model.request.UserUndistributedRequest;
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
import com.milesight.beaveriot.user.po.MenuPO;
import com.milesight.beaveriot.user.po.RoleMenuPO;
import com.milesight.beaveriot.user.po.RolePO;
import com.milesight.beaveriot.user.po.RoleResourcePO;
import com.milesight.beaveriot.user.po.UserPO;
import com.milesight.beaveriot.user.po.UserRolePO;
import com.milesight.beaveriot.user.repository.MenuRepository;
import com.milesight.beaveriot.user.repository.RoleMenuRepository;
import com.milesight.beaveriot.user.repository.RoleRepository;
import com.milesight.beaveriot.user.repository.RoleResourceRepository;
import com.milesight.beaveriot.user.repository.UserRepository;
import com.milesight.beaveriot.user.repository.UserRoleRepository;
import com.milesight.beaveriot.user.util.PageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author loong
 * @date 2024/11/19 17:49
 */
@Service
public class RoleService {

    @Autowired
    RoleRepository roleRepository;
    @Autowired
    UserRoleRepository userRoleRepository;
    @Autowired
    RoleResourceRepository roleResourceRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RoleMenuRepository roleMenuRepository;
    @Autowired
    MenuRepository menuRepository;
    @Autowired
    IDashboardFacade dashboardFacade;
    @Autowired
    IDeviceFacade deviceFacade;
    @Autowired
    IntegrationServiceProvider integrationServiceProvider;
    @Autowired
    EntityServiceProvider entityServiceProvider;

    public CreateRoleResponse createRole(CreateRoleRequest createRoleRequest) {
        String name = createRoleRequest.getName();
        if (!StringUtils.hasText(name)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("name is empty").build();
        }
        RolePO rolePO = roleRepository.findOne(filterable -> filterable.eq(RolePO.Fields.name, name)).orElse(null);
        if (rolePO != null) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("name is exist").build();
        }
        rolePO = new RolePO();
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
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("name is exist").build();
        }
        RolePO rolePO = roleRepository.findUniqueOne(filterable -> filterable.eq(RolePO.Fields.id, roleId));
        rolePO.setName(name);
        rolePO.setDescription(updateRoleRequest.getDescription());
        roleRepository.save(rolePO);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        RolePO rolePO = roleRepository.findUniqueOne(filterable -> filterable.eq(RolePO.Fields.id, roleId));
        if (rolePO == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("role is not exist").build();
        }
        if (UserConstants.SUPER_ADMIN_ROLE_NAME.equals(rolePO.getName())) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("super admin disable delete").build();
        }
        List<UserRolePO> userRolePOs = userRoleRepository.findAll(filterable -> filterable.eq(UserRolePO.Fields.roleId, roleId));
        if (!userRolePOs.isEmpty()) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("role has user").build();
        }
        roleRepository.deleteById(roleId);
        userRoleRepository.deleteByRoleId(roleId);
        roleResourceRepository.deleteByRoleId(roleId);
    }

    public Page<RoleResponse> getRoles(RoleListRequest roleListRequest) {
        Page<RolePO> rolePages = roleRepository.findAll(filterable -> filterable.likeIgnoreCase(StringUtils.hasText(roleListRequest.getKeyword()), RolePO.Fields.name, roleListRequest.getKeyword())
                , roleListRequest.toPageable());
        if (rolePages == null || rolePages.getContent().isEmpty()) {
            return Page.empty();
        }
        List<Long> roleIds = rolePages.getContent().stream().map(RolePO::getId).toList();
        List<UserRolePO> userRolePOs = userRoleRepository.findAll(filterable -> filterable.in(UserRolePO.Fields.roleId, roleIds.toArray()));
        List<RoleResourcePO> roleIntegrationPOs = roleResourceRepository.findAll(filterable -> filterable.in(RoleResourcePO.Fields.roleId, roleIds.toArray()).eq(RoleResourcePO.Fields.resourceType, ResourceType.INTEGRATION));
        Map<Long, Long> userRoleCountMap = new HashMap<>();
        if (userRolePOs != null && !userRolePOs.isEmpty()) {
            userRoleCountMap.putAll(userRolePOs.stream().collect(Collectors.groupingBy(UserRolePO::getRoleId, Collectors.counting())));
        }
        Map<Long, Long> roleIntegrationCountMap = new HashMap<>();
        if (roleIntegrationPOs != null && !roleIntegrationPOs.isEmpty()) {
            roleIntegrationCountMap.putAll(roleIntegrationPOs.stream().collect(Collectors.groupingBy(RoleResourcePO::getRoleId, Collectors.counting())));
        }
        return rolePages.map(rolePO -> {
            RoleResponse roleResponse = new RoleResponse();
            roleResponse.setRoleId(rolePO.getId().toString());
            roleResponse.setName(rolePO.getName());
            roleResponse.setCreatedAt(rolePO.getCreatedAt().toString());
            roleResponse.setUserRoleCount(userRoleCountMap.get(rolePO.getId()) == null ? 0 : Integer.parseInt(userRoleCountMap.get(rolePO.getId()).toString()));
            roleResponse.setRoleIntegrationCount(roleIntegrationCountMap.get(rolePO.getId()) == null ? 0 : Integer.parseInt(roleIntegrationCountMap.get(rolePO.getId()).toString()));
            return roleResponse;
        });
    }

    public Page<UserRoleResponse> getUsersByRoleId(Long roleId, UserRolePageRequest userRolePageRequest) {
        List<Long> searchUserIds = new ArrayList<>();
        if (StringUtils.hasText(userRolePageRequest.getKeyword())) {
            List<UserPO> userSearchPOs = userRepository.findAll(filterable -> filterable.or(filterable1 -> filterable1.likeIgnoreCase(StringUtils.hasText(userRolePageRequest.getKeyword()), UserPO.Fields.email, userRolePageRequest.getKeyword())
                            .likeIgnoreCase(StringUtils.hasText(userRolePageRequest.getKeyword()), UserPO.Fields.nickname, userRolePageRequest.getKeyword()))
            );
            if (userSearchPOs != null && !userSearchPOs.isEmpty()) {
                searchUserIds.addAll(userSearchPOs.stream().map(UserPO::getId).toList());
            }
        }
        Page<UserRolePO> userRolePOS = userRoleRepository.findAll(filterable -> filterable.eq(UserRolePO.Fields.roleId, roleId)
                        .in(!searchUserIds.isEmpty(), UserRolePO.Fields.userId, searchUserIds.toArray())
                , userRolePageRequest.toPageable());
        if (userRolePOS == null || userRolePOS.isEmpty()) {
            return Page.empty();
        }
        List<Long> userIds = userRolePOS.stream().map(UserRolePO::getUserId).toList();
        Map<Long, UserPO> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<UserPO> userPOS = userRepository.findAll(filterable -> filterable.in(UserPO.Fields.id, userIds.toArray()));
            userMap.putAll(userPOS.stream().collect(Collectors.toMap(UserPO::getId, Function.identity())));
        }
        return userRolePOS.map(userRolePO -> {
            UserRoleResponse userRoleResponse = new UserRoleResponse();
            userRoleResponse.setRoleId(userRolePO.getRoleId().toString());
            userRoleResponse.setUserId(userRolePO.getUserId().toString());
            userRoleResponse.setUserNickname(!userMap.containsKey(userRolePO.getUserId()) ? null : userMap.get(userRolePO.getUserId()).getNickname());
            userRoleResponse.setUserEmail(!userMap.containsKey(userRolePO.getUserId()) ? null : userMap.get(userRolePO.getUserId()).getEmail());
            return userRoleResponse;
        });
    }

    public List<RoleMenuResponse> getMenusByRoleId(Long roleId) {
        List<RoleMenuPO> roleMenuPOS = roleMenuRepository.findAll(filterable -> filterable.eq(RoleMenuPO.Fields.roleId, roleId));
        if (roleMenuPOS == null || roleMenuPOS.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> menuIds = roleMenuPOS.stream().map(RoleMenuPO::getMenuId).toList();
        List<MenuPO> menuPOList = menuRepository.findAll(filterable -> filterable.in(MenuPO.Fields.id, menuIds.toArray()));
        Map<Long, MenuPO> menuMap = menuPOList.stream().collect(Collectors.toMap(MenuPO::getId, Function.identity()));
        return roleMenuPOS.stream().map(roleMenuPO -> {
            if (!menuMap.containsKey(roleMenuPO.getMenuId())) {
                return null;
            }
            RoleMenuResponse roleMenuResponse = new RoleMenuResponse();
            roleMenuResponse.setMenuId(roleMenuPO.getMenuId().toString());
            roleMenuResponse.setCode(menuMap.get(roleMenuPO.getMenuId()).getCode());
            roleMenuResponse.setName(menuMap.get(roleMenuPO.getMenuId()).getName());
            roleMenuResponse.setType(menuMap.get(roleMenuPO.getMenuId()).getType());
            roleMenuResponse.setParentId(menuMap.get(roleMenuPO.getMenuId()).getParentId() == null ? null : menuMap.get(roleMenuPO.getMenuId()).getParentId().toString());
            return roleMenuResponse;
        }).filter(Objects::nonNull).toList();
    }

    public Page<RoleResourceResponse> getResourcesByRoleId(Long roleId, RoleResourceListRequest roleResourceListRequest) {
        ResourceType resourceType = roleResourceListRequest.getResourceType();
        Page<RoleResourcePO> roleResourcePOS = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                        .eq(resourceType != null, RoleResourcePO.Fields.resourceType, resourceType == null ? null : resourceType.name()),
                roleResourceListRequest.toPageable());
        if (roleResourcePOS == null || roleResourcePOS.isEmpty()) {
            return Page.empty();
        }
        return roleResourcePOS.map(roleResourcePO -> {
            RoleResourceResponse roleResourceResponse = new RoleResourceResponse();
            roleResourceResponse.setResourceId(roleResourcePO.getResourceId());
            roleResourceResponse.setResourceType(roleResourcePO.getResourceType());
            return roleResourceResponse;
        });
    }

    public Page<RoleIntegrationResponse> getIntegrationsByRoleId(Long roleId, RoleIntegrationRequest roleIntegrationRequest) {
        List<String> searchIntegrationIds = new ArrayList<>();
        if (StringUtils.hasText(roleIntegrationRequest.getKeyword())) {
            List<Integration> integrations = integrationServiceProvider.findIntegrations(f -> f.getName().toLowerCase().contains(roleIntegrationRequest.getKeyword().toLowerCase()));
            if (integrations != null && !integrations.isEmpty()) {
                List<String> integrationIds = integrations.stream().map(Integration::getId).toList();
                searchIntegrationIds.addAll(integrationIds);
            }else {
                return Page.empty();
            }
        }
        Page<RoleResourcePO> roleResourcePOS = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                        .eq(RoleResourcePO.Fields.resourceType, ResourceType.INTEGRATION.name())
                        .in(!searchIntegrationIds.isEmpty(), RoleResourcePO.Fields.resourceId, searchIntegrationIds.toArray()),
                roleIntegrationRequest.toPageable());
        if (roleResourcePOS == null || roleResourcePOS.isEmpty()) {
            return Page.empty();
        }
        List<String> integrationIds = roleResourcePOS.stream().map(RoleResourcePO::getResourceId).toList();
        List<Integration> integrations = integrationServiceProvider.findIntegrations(f -> integrationIds.contains(f.getId()));
        Map<String, Integration> integrationMap = integrations.stream().collect(Collectors.toMap(Integration::getId, Function.identity()));
        List<DeviceNameDTO> deviceNameDTOList = deviceFacade.getDeviceNameByIntegrations(integrationIds);
        Map<String, List<DeviceNameDTO>> deviceIntegrationMap = deviceNameDTOList.stream().filter(t -> t.getIntegrationConfig() != null).collect(Collectors.groupingBy(t -> t.getIntegrationConfig().getId()));
        Map<String, Long> entityCountMap = entityServiceProvider.countAllEntitiesByIntegrationIds(integrationIds);
        return roleResourcePOS.map(roleResourcePO -> {
            RoleIntegrationResponse roleIntegrationResponse = new RoleIntegrationResponse();
            roleIntegrationResponse.setIntegrationId(roleResourcePO.getResourceId());
            roleIntegrationResponse.setIntegrationName(integrationMap.get(roleResourcePO.getResourceId()) == null ? null : integrationMap.get(roleResourcePO.getResourceId()).getName());
            roleIntegrationResponse.setDeviceNum(deviceIntegrationMap.get(roleResourcePO.getResourceId()) == null ? 0L : deviceIntegrationMap.get(roleResourcePO.getResourceId()).size());
            roleIntegrationResponse.setEntityNum(entityCountMap.get(roleResourcePO.getResourceId()) == null ? 0L : entityCountMap.get(roleResourcePO.getResourceId()));
            return roleIntegrationResponse;
        });
    }

    public Page<RoleDeviceResponse> getDevicesByRoleId(Long roleId, RoleDeviceRequest roleDeviceRequest) {
        List<String> searchIntegrationIds = new ArrayList<>();
        List<Long> searchDeviceIds = new ArrayList<>();
        if (StringUtils.hasText(roleDeviceRequest.getKeyword())) {
            List<Integration> integrations = integrationServiceProvider.findIntegrations(f -> f.getName().toLowerCase().contains(roleDeviceRequest.getKeyword().toLowerCase()));
            if (integrations != null && !integrations.isEmpty()) {
                List<String> integrationIds = integrations.stream().map(Integration::getId).toList();
                searchIntegrationIds.addAll(integrationIds);
                List<DeviceNameDTO> integrationDevices = deviceFacade.getDeviceNameByIntegrations(integrationIds);
                if (integrationDevices != null && !integrationDevices.isEmpty()) {
                    searchDeviceIds.addAll(integrationDevices.stream().map(DeviceNameDTO::getId).toList());
                }
            }
            List<DeviceNameDTO> deviceNameDTOList = deviceFacade.fuzzySearchDeviceByName(roleDeviceRequest.getKeyword());
            if (deviceNameDTOList != null && !deviceNameDTOList.isEmpty()) {
                searchDeviceIds.addAll(deviceNameDTOList.stream().map(DeviceNameDTO::getId).toList());
            }
            if (searchDeviceIds.isEmpty() && searchIntegrationIds.isEmpty()) {
                return Page.empty();
            }
        }
        List<RoleResourcePO> roleIntegrationPOS = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                .eq(RoleResourcePO.Fields.resourceType, ResourceType.INTEGRATION.name())
                .in(!searchIntegrationIds.isEmpty(), RoleResourcePO.Fields.resourceId, searchIntegrationIds.toArray()));
        List<RoleResourcePO> roleDevicePOS = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                .eq(RoleResourcePO.Fields.resourceType, ResourceType.DEVICE.name())
                .in(!searchDeviceIds.isEmpty(), RoleResourcePO.Fields.resourceId, searchDeviceIds.toArray()));
        List<Long> responseDeviceIds = new ArrayList<>();
        if (roleDevicePOS != null && !roleDevicePOS.isEmpty()) {
            responseDeviceIds.addAll(roleDevicePOS.stream().map(RoleResourcePO::getResourceId).map(Long::parseLong).toList());
        }
        List<Long> responseIntegrationDeviceIds = new ArrayList<>();
        if(!roleIntegrationPOS.isEmpty()){
            List<String> integrationIds = roleIntegrationPOS.stream().map(RoleResourcePO::getResourceId).toList();
            List<DeviceNameDTO> integrationDevices = deviceFacade.getDeviceNameByIntegrations(integrationIds);
            if (integrationDevices != null && !integrationDevices.isEmpty()) {
                responseIntegrationDeviceIds.addAll(integrationDevices.stream().map(DeviceNameDTO::getId).toList());
                responseDeviceIds.addAll(integrationDevices.stream().map(DeviceNameDTO::getId).toList());
            }
        }
        if (responseDeviceIds.isEmpty()) {
            return Page.empty();
        }
        List<DeviceNameDTO> deviceNameDTOList = deviceFacade.getDeviceNameByIds(responseDeviceIds);
        Map<Long, DeviceNameDTO> deviceMap = deviceNameDTOList.stream().collect(Collectors.toMap(DeviceNameDTO::getId, Function.identity()));
        List<Long> userIds = deviceNameDTOList.stream().map(DeviceNameDTO::getUserId).filter(Objects::nonNull).toList();
        Map<Long, UserPO> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<UserPO> userPOList = userRepository.findAll(filterable -> filterable.in(UserPO.Fields.id, userIds.toArray()));
            userMap.putAll(userPOList.stream().collect(Collectors.toMap(UserPO::getId, Function.identity())));
        }
        List<RoleDeviceResponse> roleDeviceResponseList = responseDeviceIds.stream().map(deviceId -> {
            RoleDeviceResponse roleDeviceResponse = new RoleDeviceResponse();
            roleDeviceResponse.setDeviceId(deviceId.toString());
            roleDeviceResponse.setDeviceName(deviceMap.get(deviceId) == null ? null : deviceMap.get(deviceId).getName());
            roleDeviceResponse.setCreatedAt(deviceMap.get(deviceId) == null ? null : deviceMap.get(deviceId).getCreatedAt().toString());
            Long userId = deviceMap.get(deviceId) == null ? null : deviceMap.get(deviceId).getUserId();
            if (userId != null) {
                roleDeviceResponse.setUserId(userId.toString());
                roleDeviceResponse.setUserEmail(userMap.get(userId) == null ? null : userMap.get(userId).getEmail());
                roleDeviceResponse.setUserNickname(userMap.get(userId) == null ? null : userMap.get(userId).getNickname());
            }
            Integration integrationConfig = deviceMap.get(deviceId) == null ? null : deviceMap.get(deviceId).getIntegrationConfig();
            if (integrationConfig != null) {
                roleDeviceResponse.setIntegrationId(integrationConfig.getId());
                roleDeviceResponse.setIntegrationName(integrationConfig.getName());
            }
            roleDeviceResponse.setRoleIntegration(responseIntegrationDeviceIds.contains(deviceId));
            return roleDeviceResponse;
        }).toList();
        return PageConverter.convertToPage(roleDeviceResponseList, roleDeviceRequest.toPageable());
    }

    public Page<RoleDashboardResponse> getDashboardsByRoleId(Long roleId, RoleDashboardRequest roleDashboardRequest) {
        List<Long> searchDashboardIds = new ArrayList<>();
        if (StringUtils.hasText(roleDashboardRequest.getKeyword())) {
            List<DashboardDTO> dashboardPOS = dashboardFacade.getDashboardsLike(roleDashboardRequest.getKeyword(), Sort.unsorted());
            if (dashboardPOS != null && !dashboardPOS.isEmpty()) {
                searchDashboardIds.addAll(dashboardPOS.stream().map(DashboardDTO::getDashboardId).toList());
            }else{
                return Page.empty();
            }
        }
        Page<RoleResourcePO> roleResourcePOS = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                        .eq(RoleResourcePO.Fields.resourceType, ResourceType.DASHBOARD.name())
                        .in(!searchDashboardIds.isEmpty(), RoleResourcePO.Fields.resourceId, searchDashboardIds.toArray()),
                roleDashboardRequest.toPageable());
        if (roleResourcePOS == null || roleResourcePOS.isEmpty()) {
            return Page.empty();
        }
        List<Long> dashboardIds = roleResourcePOS.stream().map(RoleResourcePO::getResourceId).map(Long::parseLong).distinct().toList();
        List<DashboardDTO> dashboardDTOList = dashboardFacade.getDashboardsByIds(dashboardIds);
        Map<Long, DashboardDTO> dashboardMap = dashboardDTOList.stream().collect(Collectors.toMap(DashboardDTO::getDashboardId, Function.identity()));
        List<Long> userIds = dashboardDTOList.stream().map(DashboardDTO::getUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, UserPO> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<UserPO> userPOList = userRepository.findAll(filterable -> filterable.in(UserPO.Fields.id, userIds.toArray()));
            userMap.putAll(userPOList.stream().collect(Collectors.toMap(UserPO::getId, Function.identity())));
        }
        return roleResourcePOS.map(roleResourcePO -> {
            RoleDashboardResponse roleDashboardResponse = new RoleDashboardResponse();
            roleDashboardResponse.setDashboardId(roleResourcePO.getResourceId());
            roleDashboardResponse.setDashboardName(dashboardMap.get(Long.parseLong(roleResourcePO.getResourceId())) != null ? dashboardMap.get(Long.parseLong(roleResourcePO.getResourceId())).getDashboardName() : null);
            roleDashboardResponse.setCreatedAt(dashboardMap.get(Long.parseLong(roleResourcePO.getResourceId())) != null ? dashboardMap.get(Long.parseLong(roleResourcePO.getResourceId())).getCreatedAt().toString() : null);
            Long userId = dashboardMap.get(Long.parseLong(roleResourcePO.getResourceId())) != null ? dashboardMap.get(Long.parseLong(roleResourcePO.getResourceId())).getUserId() : null;
            roleDashboardResponse.setUserId(userId != null ? userId.toString() : null);
            if (userId != null) {
                roleDashboardResponse.setUserEmail(userMap.get(userId) != null ? userMap.get(userId).getEmail() : null);
                roleDashboardResponse.setUserNickname(userMap.get(userId) != null ? userMap.get(userId).getNickname() : null);
            }
            return roleDashboardResponse;
        });
    }

    public Page<DashboardUndistributedResponse> getUndistributedDashboards(Long roleId, DashboardUndistributedRequest dashboardUndistributedRequest) {
        List<DashboardDTO> dashboardDTOList = dashboardFacade.getDashboardsLike(dashboardUndistributedRequest.getKeyword(), dashboardUndistributedRequest.getSort().toSort());
        if (dashboardDTOList == null || dashboardDTOList.isEmpty()) {
            return Page.empty();
        }
        List<RoleResourcePO> roleResourcePOS = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                .eq(RoleResourcePO.Fields.resourceType, ResourceType.DASHBOARD.name()));
        List<Long> roleDashboardIds = roleResourcePOS.stream().map(RoleResourcePO::getResourceId).map(Long::parseLong).distinct().toList();
        List<DashboardDTO> dashboardUndistributedList = dashboardDTOList.stream().filter(dashboardDTO -> !roleDashboardIds.contains(dashboardDTO.getDashboardId())).toList();
        List<Long> userIds = dashboardUndistributedList.stream().map(DashboardDTO::getUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, UserPO> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<UserPO> userPOList = userRepository.findAll(filterable -> filterable.in(UserPO.Fields.id, userIds.toArray()));
            userMap.putAll(userPOList.stream().collect(Collectors.toMap(UserPO::getId, Function.identity())));
        }
        List<DashboardUndistributedResponse> dashboardUndistributedResponseList = dashboardUndistributedList.stream().map(dashboardDTO -> {
            DashboardUndistributedResponse dashboardListResponse = new DashboardUndistributedResponse();
            dashboardListResponse.setDashboardId(dashboardDTO.getDashboardId().toString());
            dashboardListResponse.setDashboardName(dashboardDTO.getDashboardName());
            dashboardListResponse.setCreatedAt(dashboardDTO.getCreatedAt().toString());
            dashboardListResponse.setUserId(dashboardDTO.getUserId().toString());
            dashboardListResponse.setUserEmail(userMap.get(dashboardDTO.getUserId()) == null ? null : userMap.get(dashboardDTO.getUserId()).getEmail());
            dashboardListResponse.setUserNickname(userMap.get(dashboardDTO.getUserId()) == null ? null : userMap.get(dashboardDTO.getUserId()).getNickname());
            return dashboardListResponse;
        }).collect(Collectors.toList());
        return PageConverter.convertToPage(dashboardUndistributedResponseList, dashboardUndistributedRequest.toPageable());
    }

    public Page<UserUndistributedResponse> getUndistributedUsers(Long roleId, UserUndistributedRequest userUndistributedRequest) {
        List<UserPO> userPOS = userRepository.findAll(filterable -> filterable.likeIgnoreCase(StringUtils.hasText(userUndistributedRequest.getKeyword()), UserPO.Fields.nickname, userUndistributedRequest.getKeyword()), userUndistributedRequest.getSort().toSort());
        if (userPOS == null || userPOS.isEmpty()) {
            return Page.empty();
        }
        List<UserRolePO> userRolePOS = userRoleRepository.findAll(filterable -> filterable.eq(UserRolePO.Fields.roleId, roleId));
        List<Long> userIds = userRolePOS.stream().map(UserRolePO::getUserId).distinct().toList();
        List<UserUndistributedResponse> userUndistributedResponseList = userPOS.stream().filter(userPO -> !userIds.contains(userPO.getId())).map(userPO -> {
            UserUndistributedResponse userUndistributedResponse = new UserUndistributedResponse();
            userUndistributedResponse.setUserId(userPO.getId().toString());
            userUndistributedResponse.setEmail(userPO.getEmail());
            userUndistributedResponse.setNickname(userPO.getNickname());
            return userUndistributedResponse;
        }).collect(Collectors.toList());
        return PageConverter.convertToPage(userUndistributedResponseList, userUndistributedRequest.toPageable());
    }

    public Page<IntegrationUndistributedResponse> getUndistributedIntegrations(Long roleId, IntegrationUndistributedRequest integrationUndistributedRequest) {
        List<Integration> integrations = new ArrayList<>();
        if (StringUtils.hasText(integrationUndistributedRequest.getKeyword())) {
            integrations.addAll(integrationServiceProvider.findIntegrations(f -> f.getName().toLowerCase().contains(integrationUndistributedRequest.getKeyword().toLowerCase())));
        } else {
            integrations.addAll(integrationServiceProvider.findIntegrations());
        }
        List<RoleResourcePO> roleResourcePOS = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                .eq(RoleResourcePO.Fields.resourceType, ResourceType.INTEGRATION.name()));
        List<String> roleIntegrationIds = roleResourcePOS.stream().map(RoleResourcePO::getResourceId).toList();
        List<IntegrationUndistributedResponse> integrationUndistributedResponseList = integrations.stream().filter(integration -> !roleIntegrationIds.contains(integration.getId())).map(integration -> {
            IntegrationUndistributedResponse integrationUndistributedResponse = new IntegrationUndistributedResponse();
            integrationUndistributedResponse.setIntegrationId(integration.getId());
            integrationUndistributedResponse.setIntegrationName(integration.getName());
            return integrationUndistributedResponse;
        }).toList();
        return PageConverter.convertToPage(integrationUndistributedResponseList, integrationUndistributedRequest.toPageable());
    }

    public Page<DeviceUndistributedResponse> getUndistributedDevices(Long roleId, DeviceUndistributedRequest deviceUndistributedRequest) {
        List<Integration> integrations = integrationServiceProvider.findIntegrations().stream().toList();
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
                Integration integration = deviceNameDTO.getIntegrationConfig();
                if (integration != null) {
                    if (integration.getName().toLowerCase().contains(deviceUndistributedRequest.getKeyword().toLowerCase())) {
                        return true;
                    }
                }
                return false;
            }).toList();
        }
        if (deviceNameDTOList.isEmpty()) {
            return Page.empty();
        }
        List<Long> deviceUserIds = deviceNameDTOList.stream().map(DeviceNameDTO::getUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, UserPO> userMap = new HashMap<>();
        if (!deviceUserIds.isEmpty()) {
            List<UserPO> userPOS = userRepository.findAll(filterable -> filterable.in(UserPO.Fields.id, deviceUserIds.toArray()));
            userMap.putAll(userPOS.stream().collect(Collectors.toMap(UserPO::getId, Function.identity())));
        }
        List<Long> roleDeviceIds = new ArrayList<>();
        List<RoleResourcePO> roleIntegrationPOS = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                .eq(RoleResourcePO.Fields.resourceType, ResourceType.INTEGRATION.name()));
        List<String> roleIntegrationIds = roleIntegrationPOS.stream().map(RoleResourcePO::getResourceId).toList();
        if (!roleIntegrationIds.isEmpty()) {
            List<DeviceNameDTO> deviceNameDTOListByIntegration = deviceFacade.getDeviceNameByKey(roleIntegrationIds);
            roleDeviceIds.addAll(deviceNameDTOListByIntegration.stream().map(DeviceNameDTO::getId).toList());
        }
        List<RoleResourcePO> roleDevicePOS = roleResourceRepository.findAll(filterable -> filterable.eq(RoleResourcePO.Fields.roleId, roleId)
                .eq(RoleResourcePO.Fields.resourceType, ResourceType.DEVICE.name()));
        List<Long> roleDeviceIdsByDevice = roleDevicePOS.stream().map(RoleResourcePO::getResourceId).map(Long::parseLong).toList();
        if (!roleDeviceIdsByDevice.isEmpty()) {
            roleDeviceIds.addAll(roleDeviceIdsByDevice);
        }
        List<DeviceUndistributedResponse> deviceUndistributedResponseList = deviceNameDTOList.stream().filter(deviceNameDTO -> !roleDeviceIds.contains(deviceNameDTO.getId())).map(deviceNameDTO -> {
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
            Integration integrationConfig = deviceNameDTO.getIntegrationConfig();
            if (integrationConfig != null) {
                deviceUndistributedResponse.setIntegrationId(integrationConfig.getId());
                deviceUndistributedResponse.setIntegrationName(integrationConfig.getName());
            }
            return deviceUndistributedResponse;
        }).toList();
        return PageConverter.convertToPage(deviceUndistributedResponseList, deviceUndistributedRequest.toPageable());
    }

    @Transactional(rollbackFor = Exception.class)
    public void associateUser(Long roleId, UserRoleRequest userRoleRequest) {
        List<Long> userIds = userRoleRequest.getUserIds();
        if (userIds != null && !userIds.isEmpty()) {
            List<Long> finalUserIds = userIds;
            List<UserPO> userPOS = userRepository.findAll(filterable -> filterable.in(UserPO.Fields.id, finalUserIds.toArray()));
            if (userPOS != null && !userPOS.isEmpty()) {
                userIds = userPOS.stream().map(UserPO::getId).toList();
                List<UserRolePO> userRolePOS = userIds.stream().map(userId -> {
                    UserRolePO userRolePO = new UserRolePO();
                    userRolePO.setId(SnowflakeUtil.nextId());
                    userRolePO.setRoleId(roleId);
                    userRolePO.setUserId(userId);
                    return userRolePO;
                }).toList();
                userRoleRepository.saveAll(userRolePOS);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void disassociateUser(Long roleId, UserRoleRequest userRoleRequest) {
        List<Long> userIds = userRoleRequest.getUserIds();
        if (userIds != null && !userIds.isEmpty()) {
            List<UserRolePO> userRolePOS = userRoleRepository.findAll(filterable -> filterable.in(UserRolePO.Fields.userId, userIds.toArray()).eq(UserRolePO.Fields.roleId, roleId));
            if (userRolePOS != null && !userRolePOS.isEmpty()) {
                userRoleRepository.deleteAll(userRolePOS);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void associateResource(Long roleId, RoleResourceRequest roleResourceRequest) {
        List<RoleResourceRequest.Resource> resources = roleResourceRequest.getResources();
        if (resources == null || resources.isEmpty()) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("resources is empty").build();
        }
        List<RoleResourcePO> roleResourcePOS = resources.stream().map(resource -> {
            RoleResourcePO roleResourcePO = new RoleResourcePO();
            roleResourcePO.setId(SnowflakeUtil.nextId());
            roleResourcePO.setRoleId(roleId);
            roleResourcePO.setResourceId(resource.getId());
            roleResourcePO.setResourceType(resource.getType());
            return roleResourcePO;
        }).toList();
        roleResourceRepository.saveAll(roleResourcePOS);
    }

    @Transactional(rollbackFor = Exception.class)
    public void disassociateResource(Long roleId, RoleResourceRequest roleResourceRequest) {
        List<RoleResourceRequest.Resource> resources = roleResourceRequest.getResources();
        if (resources == null || resources.isEmpty()) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("resources is empty").build();
        }
        Map<ResourceType, List<RoleResourceRequest.Resource>> resourceMap = resources.stream().collect(Collectors.groupingBy(RoleResourceRequest.Resource::getType));
        resourceMap.forEach((resourceType, resourceList) -> {
            List<String> resourceIds = resourceList.stream().map(RoleResourceRequest.Resource::getId).toList();
            List<RoleResourcePO> roleResourcePOS = roleResourceRepository.findAll(filterable -> filterable.in(RoleResourcePO.Fields.resourceId, resourceIds.toArray()).eq(RoleResourcePO.Fields.roleId, roleId));
            if (roleResourcePOS != null && !roleResourcePOS.isEmpty()) {
                roleResourceRepository.deleteAll(roleResourcePOS);
            }
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void associateMenu(Long roleId, RoleMenuRequest roleMenuRequest) {
        List<Long> menuIds = roleMenuRequest.getMenuIds();
        roleMenuRepository.deleteByRoleId(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            List<Long> finalMenuIds = menuIds;
            List<MenuPO> menuPOS = menuRepository.findAll(filterable -> filterable.in(MenuPO.Fields.id, finalMenuIds.toArray()));
            if (menuPOS != null && !menuPOS.isEmpty()) {
                menuIds = menuPOS.stream().map(MenuPO::getId).toList();
                List<RoleMenuPO> roleMenuPOS = menuIds.stream().map(menuId -> {
                    RoleMenuPO roleMenuPO = new RoleMenuPO();
                    roleMenuPO.setId(SnowflakeUtil.nextId());
                    roleMenuPO.setRoleId(roleId);
                    roleMenuPO.setMenuId(menuId);
                    return roleMenuPO;
                }).toList();
                roleMenuRepository.saveAll(roleMenuPOS);
            }
        }
    }

}
