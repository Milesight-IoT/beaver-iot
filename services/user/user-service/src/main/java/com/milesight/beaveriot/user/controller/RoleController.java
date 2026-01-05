package com.milesight.beaveriot.user.controller;

import com.milesight.beaveriot.base.page.GenericQueryPageRequest;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.permission.aspect.AdminPermission;
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
import com.milesight.beaveriot.user.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author loong
 * @date 2024/11/19 17:50
 */
@RestController
@RequestMapping("/user/roles")
public class RoleController {

    @Autowired
    RoleService roleService;

    @AdminPermission
    @PostMapping("")
    public ResponseBody<CreateRoleResponse> createRole(@RequestBody @Valid CreateRoleRequest createRoleRequest) {
        CreateRoleResponse createRoleResponse = roleService.createRole(createRoleRequest);
        return ResponseBuilder.success(createRoleResponse);
    }

    @AdminPermission
    @PutMapping("/{roleId}")
    public ResponseBody<Void> updateRole(@PathVariable("roleId") Long roleId, @RequestBody @Valid UpdateRoleRequest updateRoleRequest) {
        roleService.updateRole(roleId, updateRoleRequest);
        return ResponseBuilder.success();
    }

    @AdminPermission
    @DeleteMapping("/{roleId}")
    public ResponseBody<Void> deleteRole(@PathVariable("roleId") Long roleId) {
        roleService.deleteRole(roleId);
        return ResponseBuilder.success();
    }

    @PostMapping("/search")
    public ResponseBody<Page<RoleResponse>> getRoles(@RequestBody GenericQueryPageRequest roleListRequest) {
        Page<RoleResponse> roleResponses = roleService.getRoles(roleListRequest);
        return ResponseBuilder.success(roleResponses);
    }

    @PostMapping("/{roleId}/members")
    public ResponseBody<Page<UserRoleResponse>> getUsersByRoleId(@PathVariable("roleId") Long roleId, @RequestBody GenericQueryPageRequest userRolePageRequest) {
        Page<UserRoleResponse> userRoles = roleService.getUsersByRoleId(roleId, userRolePageRequest);
        return ResponseBuilder.success(userRoles);
    }

    @GetMapping("/{roleId}/menus")
    public ResponseBody<List<RoleMenuResponse>> getMenusByRoleId(@PathVariable("roleId") Long roleId) {
        List<RoleMenuResponse> roleMenus = roleService.getMenusByRoleId(roleId);
        return ResponseBuilder.success(roleMenus);
    }

    @PostMapping("/{roleId}/resources")
    public ResponseBody<Page<RoleResourceResponse>> getResourcesByRoleId(@PathVariable("roleId") Long roleId, @RequestBody RoleResourceListRequest roleResourceListRequest) {
        Page<RoleResourceResponse> roleResources = roleService.getResourcesByRoleId(roleId, roleResourceListRequest);
        return ResponseBuilder.success(roleResources);
    }

    @PostMapping("/{roleId}/integrations")
    public ResponseBody<Page<RoleIntegrationResponse>> getIntegrationsByRoleId(@PathVariable("roleId") Long roleId, @RequestBody GenericQueryPageRequest roleIntegrationRequest) {
        Page<RoleIntegrationResponse> roleIntegrations = roleService.getIntegrationsByRoleId(roleId, roleIntegrationRequest);
        return ResponseBuilder.success(roleIntegrations);
    }

    @PostMapping("/{roleId}/devices")
    public ResponseBody<Page<RoleDeviceResponse>> getDevicesByRoleId(@PathVariable("roleId") Long roleId, @RequestBody GenericQueryPageRequest roleDeviceRequest) {
        Page<RoleDeviceResponse> roleDevices = roleService.getDevicesByRoleId(roleId, roleDeviceRequest);
        return ResponseBuilder.success(roleDevices);
    }

    @PostMapping("/{roleId}/dashboards")
    public ResponseBody<Page<RoleDashboardResponse>> getDashboardsByRoleId(@PathVariable("roleId") Long roleId, @RequestBody GenericQueryPageRequest roleDashboardRequest) {
        Page<RoleDashboardResponse> roleDashboards = roleService.getDashboardsByRoleId(roleId, roleDashboardRequest);
        return ResponseBuilder.success(roleDashboards);
    }

    @PostMapping("/{roleId}/undistributed-dashboards")
    public ResponseBody<Page<DashboardUndistributedResponse>> getUndistributedDashboards(@PathVariable("roleId") Long roleId, @RequestBody GenericQueryPageRequest dashboardUndistributedRequest) {
        Page<DashboardUndistributedResponse> dashboardResponseList = roleService.getUndistributedDashboards(roleId, dashboardUndistributedRequest);
        return ResponseBuilder.success(dashboardResponseList);
    }

    @PostMapping("/{roleId}/undistributed-users")
    public ResponseBody<Page<UserUndistributedResponse>> getUndistributedUsers(@PathVariable("roleId") Long roleId, @RequestBody GenericQueryPageRequest userUndistributedRequest) {
        Page<UserUndistributedResponse> userUndistributedResponses = roleService.getUndistributedUsers(roleId, userUndistributedRequest);
        return ResponseBuilder.success(userUndistributedResponses);
    }

    @PostMapping("/{roleId}/undistributed-integrations")
    public ResponseBody<Page<IntegrationUndistributedResponse>> getUndistributedIntegrations(@PathVariable("roleId") Long roleId, @RequestBody GenericQueryPageRequest integrationUndistributedRequest) {
        Page<IntegrationUndistributedResponse> integrationUndistributedResponses = roleService.getUndistributedIntegrations(roleId, integrationUndistributedRequest);
        return ResponseBuilder.success(integrationUndistributedResponses);
    }

    @PostMapping("/{roleId}/undistributed-devices")
    public ResponseBody<Page<DeviceUndistributedResponse>> getUndistributedDevices(@PathVariable("roleId") Long roleId, @RequestBody GenericQueryPageRequest deviceUndistributedRequest) {
        Page<DeviceUndistributedResponse> deviceUndistributedResponses = roleService.getUndistributedDevices(roleId, deviceUndistributedRequest);
        return ResponseBuilder.success(deviceUndistributedResponses);
    }

    @AdminPermission
    @PostMapping("/{roleId}/associate-user")
    public ResponseBody<Void> associateUser(@PathVariable("roleId") Long roleId, @RequestBody UserRoleRequest userRoleRequest) {
        roleService.associateUser(roleId, userRoleRequest);
        return ResponseBuilder.success();
    }

    @AdminPermission
    @PostMapping("/{roleId}/disassociate-user")
    public ResponseBody<Void> disassociateUser(@PathVariable("roleId") Long roleId, @RequestBody UserRoleRequest userRoleRequest) {
        roleService.disassociateUser(roleId, userRoleRequest);
        return ResponseBuilder.success();
    }

    @AdminPermission
    @PostMapping("/{roleId}/associate-resource")
    public ResponseBody<Void> associateResource(@PathVariable("roleId") Long roleId, @RequestBody RoleResourceRequest roleResourceRequest) {
        roleService.associateResource(roleId, roleResourceRequest);
        return ResponseBuilder.success();
    }

    @AdminPermission
    @PostMapping("/{roleId}/disassociate-resource")
    public ResponseBody<Void> disassociateResource(@PathVariable("roleId") Long roleId, @RequestBody RoleResourceRequest roleResourceRequest) {
        roleService.disassociateResource(roleId, roleResourceRequest);
        return ResponseBuilder.success();
    }

    @AdminPermission
    @PostMapping("/{roleId}/associate-menu")
    public ResponseBody<Void> associateMenu(@PathVariable("roleId") Long roleId, @RequestBody RoleMenuRequest roleMenuRequest) {
        roleService.associateMenu(roleId, roleMenuRequest);
        return ResponseBuilder.success();
    }
}
