package com.milesight.beaveriot.device.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.device.dto.DeviceResponseData;
import com.milesight.beaveriot.context.integration.model.DeviceLocation;
import com.milesight.beaveriot.device.location.service.DeviceLocationService;
import com.milesight.beaveriot.device.model.request.*;
import com.milesight.beaveriot.device.model.response.DeviceCanvasResponse;
import com.milesight.beaveriot.device.model.response.DeviceDetailResponse;
import com.milesight.beaveriot.device.model.response.DeviceLocationResponse;
import com.milesight.beaveriot.device.service.DeviceCanvasService;
import com.milesight.beaveriot.device.service.DeviceService;
import com.milesight.beaveriot.permission.aspect.OperationPermission;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/device")
public class DeviceController {
    @Autowired
    DeviceService deviceService;

    @Autowired
    DeviceCanvasService deviceCanvasService;

    @Autowired
    DeviceLocationService deviceLocationService;

    @OperationPermission(codes = OperationPermissionCode.DEVICE_ADD)
    @PostMapping
    public ResponseBody<String> createDevice(@RequestBody @Valid CreateDeviceRequest createDeviceRequest) {
        deviceService.createDevice(createDeviceRequest);
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = {OperationPermissionCode.DEVICE_VIEW, OperationPermissionCode.DASHBOARD_VIEW, OperationPermissionCode.DASHBOARD_EDIT})
    @PostMapping("/search")
    public ResponseBody<Page<DeviceResponseData>> searchDevice(@RequestBody @Valid SearchDeviceRequest searchDeviceRequest) {
        return ResponseBuilder.success(deviceService.searchDevice(searchDeviceRequest));
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_EDIT)
    @PutMapping("/{deviceId}")
    public ResponseBody<Void> updateDevice(@PathVariable("deviceId") Long deviceId, @RequestBody @Valid UpdateDeviceRequest updateDeviceRequest) {
        deviceService.updateDevice(deviceId, updateDeviceRequest);
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_DELETE)
    @PostMapping("/batch-delete")
    public ResponseBody<Void> batchDeleteDevices(@RequestBody BatchDeleteDeviceRequest batchDeleteDeviceRequest) {
        deviceService.batchDeleteDevices(batchDeleteDeviceRequest.getDeviceIdList());
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_VIEW)
    @GetMapping("/{deviceId}")
    public ResponseBody<DeviceDetailResponse> getDeviceDetail(@PathVariable("deviceId") Long deviceId) {
        return ResponseBuilder.success(deviceService.getDeviceDetail(deviceId));
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_EDIT)
    @PostMapping("/move-to-group")
    public ResponseBody<Void> updateDevice(@RequestBody @Valid MoveDeviceToGroupRequest request) {
        deviceService.moveDeviceToGroup(request);
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = {OperationPermissionCode.DEVICE_VIEW, OperationPermissionCode.DASHBOARD_VIEW})
    @GetMapping("/{deviceId}/canvas")
    public ResponseBody<DeviceCanvasResponse> getDeviceCanvas(@PathVariable("deviceId") Long deviceId) {
        return ResponseBuilder.success(deviceCanvasService.getOrCreateDeviceCanvas(deviceId));
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_EDIT)
    @PutMapping("/{deviceId}/location")
    public ResponseBody<Void> setDeviceLocation(@PathVariable("deviceId") Long deviceId, @RequestBody SetDeviceLocationRequest request) {
        Device device = deviceService.findById(deviceId);
        if (device != null) {
            DeviceLocation location = request.buildLocation();
            deviceLocationService.setLocation(device, location);
        }
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_VIEW)
    @GetMapping("/{deviceId}/location")
    public ResponseBody<DeviceLocationResponse> getDeviceLocation(@PathVariable("deviceId") Long deviceId) {
        Device device = deviceService.findById(deviceId);
        DeviceLocation location = deviceLocationService.getLocation(device);
        DeviceLocationResponse response = new DeviceLocationResponse();
        if (location != null) {
            response.setLatitude(location.getLatitude());
            response.setLongitude(location.getLongitude());
            response.setAddress(location.getAddress());
        }
        return ResponseBuilder.success(response);
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_EDIT)
    @PostMapping("/{deviceId}/clear-location")
    public ResponseBody<Void> clearDeviceLocation(@PathVariable("deviceId") Long deviceId) {
        Device device = deviceService.findById(deviceId);
        if (device != null) {
            deviceLocationService.clearLocation(device);
        }
        return ResponseBuilder.success();
    }
}