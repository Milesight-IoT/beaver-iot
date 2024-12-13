package com.milesight.beaveriot.integration.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.integration.model.request.SearchIntegrationRequest;
import com.milesight.beaveriot.integration.model.response.IntegrationDetailData;
import com.milesight.beaveriot.integration.model.response.SearchIntegrationResponseData;
import com.milesight.beaveriot.integration.service.IntegrationService;
import com.milesight.beaveriot.permission.aspect.OperationPermission;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/integration")
public class IntegrationController {
    @Autowired
    IntegrationService integrationService;

    @OperationPermission(codes = {OperationPermissionCode.DEVICE_ADD,OperationPermissionCode.INTEGRATION_VIEW})
    @PostMapping("/search")
    public ResponseBody<List<SearchIntegrationResponseData>> searchIntegration(@RequestBody SearchIntegrationRequest searchIntegrationRequest) {
        return ResponseBuilder.success(integrationService.searchIntegration(searchIntegrationRequest));
    }

    @OperationPermission(codes = {OperationPermissionCode.DEVICE_ADD,OperationPermissionCode.INTEGRATION_VIEW})
    @GetMapping("/{integrationId}")
    public ResponseBody<IntegrationDetailData> getIntegrationDetail(@PathVariable("integrationId") String integrationId) {
        return ResponseBuilder.success(integrationService.getDetailData(integrationId));
    }
}
