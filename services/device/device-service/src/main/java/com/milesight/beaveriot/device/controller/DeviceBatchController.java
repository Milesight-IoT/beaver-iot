package com.milesight.beaveriot.device.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.device.model.request.BatchDeviceTemplateRequest;
import com.milesight.beaveriot.device.service.DeviceBatchService;
import com.milesight.beaveriot.permission.aspect.OperationPermission;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DeviceBatchController class.
 *
 * @author simon
 * @date 2025/6/26
 */
@RestController
@RequestMapping("/device-batch")
public class DeviceBatchController {
    @Autowired
    DeviceBatchService deviceBatchService;

    @OperationPermission(codes = OperationPermissionCode.DEVICE_ADD)
    @PostMapping("/template")
    public ResponseEntity<byte[]> createDevice(@RequestBody @Valid BatchDeviceTemplateRequest request) {
        String fileName = request.getIntegration() + ".xlsx";
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); // From https://en.wikipedia.org/wiki/Media_type#Common_examples

        return ResponseEntity.ok()
                .headers(headers)
                .body(deviceBatchService.generateTemplate(request.getIntegration()));
    }
}
