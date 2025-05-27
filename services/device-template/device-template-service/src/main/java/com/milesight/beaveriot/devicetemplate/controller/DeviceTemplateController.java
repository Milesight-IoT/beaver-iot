package com.milesight.beaveriot.devicetemplate.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.context.api.DeviceTemplateParserProvider;
import com.milesight.beaveriot.context.model.request.SearchDeviceTemplateRequest;
import com.milesight.beaveriot.context.model.response.DeviceTemplateDetailResponse;
import com.milesight.beaveriot.context.model.response.DeviceTemplateResponseData;
import com.milesight.beaveriot.devicetemplate.model.request.*;
import com.milesight.beaveriot.devicetemplate.model.response.DeviceTemplateDefaultContent;
import com.milesight.beaveriot.devicetemplate.service.DeviceTemplateService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/device-template")
public class DeviceTemplateController {
    private final DeviceTemplateService deviceTemplateService;
    private final  DeviceTemplateParserProvider deviceTemplateParserProvider;

    public DeviceTemplateController(DeviceTemplateService deviceTemplateService, DeviceTemplateParserProvider deviceTemplateParserProvider) {
        this.deviceTemplateService = deviceTemplateService;
        this.deviceTemplateParserProvider = deviceTemplateParserProvider;
    }

    @PostMapping
    public ResponseBody<String> createDeviceTemplate(@RequestBody CreateDeviceTemplateRequest createDeviceTemplateRequest) {
        deviceTemplateService.createDeviceTemplate(createDeviceTemplateRequest);
        return ResponseBuilder.success();
    }

    @PostMapping("/search")
    public ResponseBody<Page<DeviceTemplateResponseData>> searchDeviceTemplate(@RequestBody SearchDeviceTemplateRequest searchDeviceTemplateRequest) {
        return ResponseBuilder.success(deviceTemplateService.searchDeviceTemplate(searchDeviceTemplateRequest));
    }

    @PostMapping("/{deviceTemplateId}/test")
    public ResponseBody<Void> testDeviceTemplate(@PathVariable("deviceTemplateId") Long deviceTemplateId, @RequestBody TestDeviceTemplateRequest testDeviceTemplateRequest) {
        String deviceTemplateContent = deviceTemplateService.getDeviceTemplateDetail(deviceTemplateId).getContent();
        deviceTemplateParserProvider.discover(testDeviceTemplateRequest.getIntegration(), testDeviceTemplateRequest.getTestData(), deviceTemplateId, deviceTemplateContent);
        return ResponseBuilder.success();
    }

    @PutMapping("/{deviceTemplateId}")
    public ResponseBody<Void> updateDeviceTemplate(@PathVariable("deviceTemplateId") Long deviceTemplateId, @RequestBody UpdateDeviceTemplateRequest updateDeviceTemplateRequest) {
        deviceTemplateService.updateDeviceTemplate(deviceTemplateId, updateDeviceTemplateRequest);
        return ResponseBuilder.success();
    }

    @PostMapping("/batch-delete")
    public ResponseBody<Void> batchDeleteDeviceTemplates(@RequestBody BatchDeleteDeviceTemplateRequest batchDeleteDeviceTemplateRequest) {
        deviceTemplateService.batchDeleteDeviceTemplates(batchDeleteDeviceTemplateRequest.getIdList());
        return ResponseBuilder.success();
    }

    @GetMapping("/{deviceTemplateId}")
    public ResponseBody<DeviceTemplateDetailResponse> getDeviceDetail(@PathVariable("deviceTemplateId") Long deviceTemplateId) {
        return ResponseBuilder.success(deviceTemplateService.getDeviceTemplateDetail(deviceTemplateId));
    }

    @PostMapping("/validate")
    public ResponseBody<Void> validate(@RequestBody ValidateDeviceTemplateRequest validateDeviceTemplateRequest) {
        deviceTemplateParserProvider.validate(validateDeviceTemplateRequest.getContent());
        return ResponseBuilder.success();
    }

    @GetMapping("/content/default")
    public ResponseBody<DeviceTemplateDefaultContent> getDefaultDeviceTemplateContent() {
        return ResponseBuilder.success(DeviceTemplateDefaultContent.build(deviceTemplateParserProvider.getDefaultDeviceTemplateContent()));
    }
}
