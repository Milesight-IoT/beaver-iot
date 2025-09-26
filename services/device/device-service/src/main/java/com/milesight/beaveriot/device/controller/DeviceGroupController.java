package com.milesight.beaveriot.device.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.device.model.request.CreateDeviceGroupRequest;
import com.milesight.beaveriot.device.model.request.SearchDeviceGroupRequest;
import com.milesight.beaveriot.device.model.response.DeviceGroupNumberResponse;
import com.milesight.beaveriot.device.model.response.DeviceGroupResponseData;
import com.milesight.beaveriot.device.service.DeviceGroupService;
import com.milesight.beaveriot.permission.aspect.OperationPermission;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * DeviceGroupController class.
 *
 * @author simon
 * @date 2025/6/25
 */
@RestController
@RequestMapping("/device-group")
public class DeviceGroupController {
    @Autowired
    DeviceGroupService deviceGroupService;

    @OperationPermission(codes = {OperationPermissionCode.DEVICE_VIEW, OperationPermissionCode.DASHBOARD_EDIT})
    @PostMapping("/search")
    public ResponseBody<Page<DeviceGroupResponseData>> searchGroup(@RequestBody @Valid SearchDeviceGroupRequest request) {
        return ResponseBuilder.success(deviceGroupService.search(request));
    }

    @OperationPermission(codes = {OperationPermissionCode.DEVICE_VIEW, OperationPermissionCode.DASHBOARD_EDIT})
    @GetMapping("/number")
    public ResponseBody<DeviceGroupNumberResponse> countGroup() {
        return ResponseBuilder.success(new DeviceGroupNumberResponse(deviceGroupService.countDeviceGroup()));
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_GROUP_MANAGE)
    @PostMapping("")
    public ResponseBody<Void> addGroup(@RequestBody @Valid CreateDeviceGroupRequest request) {
        deviceGroupService.getOrCreateDeviceGroup(request, true);
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_GROUP_MANAGE)
    @PutMapping("/{groupId}")
    public ResponseBody<Void> updateGroup(@PathVariable("groupId") Long groupId, @RequestBody @Valid CreateDeviceGroupRequest request) {
        deviceGroupService.updateDeviceGroup(groupId, request);
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_GROUP_MANAGE)
    @DeleteMapping("/{groupId}")
    public ResponseBody<Void> deleteGroup(@PathVariable("groupId") Long groupId) {
        deviceGroupService.deleteDeviceGroup(groupId);
        return ResponseBuilder.success();
    }
}
