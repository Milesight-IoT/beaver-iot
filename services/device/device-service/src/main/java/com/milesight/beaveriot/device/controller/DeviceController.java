package com.milesight.beaveriot.device.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.device.model.request.*;
import com.milesight.beaveriot.device.model.response.DeviceDetailResponse;
import com.milesight.beaveriot.device.model.response.DeviceResponseData;
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

    @OperationPermission(codes = OperationPermissionCode.DEVICE_ADD)
    @PostMapping
    public ResponseBody<String> createDevice(@RequestBody @Valid CreateDeviceRequest createDeviceRequest) {
        deviceService.createDevice(createDeviceRequest);
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_VIEW)
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
}
