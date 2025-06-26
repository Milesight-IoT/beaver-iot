package com.milesight.beaveriot.device.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.device.model.request.CreateDeviceGroupRequest;
import com.milesight.beaveriot.device.model.request.SearchDeviceGroupRequest;
import com.milesight.beaveriot.device.model.response.DeviceGroupResponseData;
import com.milesight.beaveriot.device.service.DeviceGroupService;
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

    @PostMapping("/search")
    public ResponseBody<Page<DeviceGroupResponseData>> searchGroup(@RequestBody @Valid SearchDeviceGroupRequest request) {
        // TODO
        return ResponseBuilder.success();
    }

    @PostMapping("")
    public ResponseBody<Void> addGroup(@RequestBody @Valid CreateDeviceGroupRequest request) {
        // TODO
        return ResponseBuilder.success();
    }

    @PutMapping("/{groupId}")
    public ResponseBody<Void> updateGroup(@PathVariable("groupId") Long groupId, @RequestBody @Valid CreateDeviceGroupRequest request) {
        // TODO
        return ResponseBuilder.success();
    }

    @DeleteMapping("/{groupId}")
    public ResponseBody<Void> deleteGroup(@PathVariable("groupId") Long groupId) {
        // TODO
        return ResponseBuilder.success();
    }

    @PostMapping("/device-transfer")
    public ResponseBody<Void> transferDevice() {
        // TODO
        return ResponseBuilder.success();
    }

    @DeleteMapping("/device-remove")
    public ResponseBody<Void> removeDeviceFromGroup(@PathVariable("groupId") Long groupId, @PathVariable("deviceId") Long deviceId) {
        // TODO
        return ResponseBuilder.success();
    }
}
