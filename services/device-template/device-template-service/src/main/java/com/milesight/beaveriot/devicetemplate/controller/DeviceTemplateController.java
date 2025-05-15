package com.milesight.beaveriot.devicetemplate.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.devicetemplate.model.request.BatchDeleteDeviceTemplateRequest;
import com.milesight.beaveriot.devicetemplate.model.request.CreateDeviceTemplateRequest;
import com.milesight.beaveriot.devicetemplate.model.request.SearchDeviceTemplateRequest;
import com.milesight.beaveriot.devicetemplate.model.request.UpdateDeviceTemplateRequest;
import com.milesight.beaveriot.devicetemplate.model.response.DeviceTemplateDetailResponse;
import com.milesight.beaveriot.devicetemplate.model.response.DeviceTemplateResponseData;
import com.milesight.beaveriot.devicetemplate.service.DeviceTemplateService;
import com.milesight.beaveriot.permission.aspect.OperationPermission;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/device-template")
public class DeviceTemplateController {

    @Autowired
    DeviceTemplateService deviceTemplateService;

    @OperationPermission(codes = OperationPermissionCode.DEVICE_ADD)
    @PostMapping
    public ResponseBody<String> createDeviceTemplate(@RequestBody CreateDeviceTemplateRequest createDeviceTemplateRequest) {
        deviceTemplateService.createDeviceTemplate(createDeviceTemplateRequest);
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_VIEW)
    @PostMapping("/search")
    public ResponseBody<Page<DeviceTemplateResponseData>> searchDeviceTemplate(@RequestBody SearchDeviceTemplateRequest searchDeviceTemplateRequest) {
        return ResponseBuilder.success(deviceTemplateService.searchDeviceTemplate(searchDeviceTemplateRequest));
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_EDIT)
    @PutMapping("/{deviceTemplateId}")
    public ResponseBody<Void> updateDeviceTemplate(@PathVariable("deviceTemplateId") Long deviceTemplateId, @RequestBody UpdateDeviceTemplateRequest updateDeviceTemplateRequest) {
        deviceTemplateService.updateDeviceTemplate(deviceTemplateId, updateDeviceTemplateRequest);
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_DELETE)
    @PostMapping("/batch-delete")
    public ResponseBody<Void> batchDeleteDeviceTemplates(@RequestBody BatchDeleteDeviceTemplateRequest batchDeleteDeviceTemplateRequest) {
        deviceTemplateService.batchDeleteDeviceTemplates(batchDeleteDeviceTemplateRequest.getIdList());
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_VIEW)
    @GetMapping("/{deviceTemplateId}")
    public ResponseBody<DeviceTemplateDetailResponse> getDeviceDetail(@PathVariable("deviceTemplateId") Long deviceId) {
        return ResponseBuilder.success(deviceTemplateService.getDeviceTemplateDetail(deviceId));
    }
}
