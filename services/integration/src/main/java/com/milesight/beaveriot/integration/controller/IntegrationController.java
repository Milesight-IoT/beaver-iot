package com.milesight.beaveriot.integration.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.integration.model.request.SearchIntegrationRequest;
import com.milesight.beaveriot.integration.model.response.IntegrationDetailData;
import com.milesight.beaveriot.integration.model.response.SearchIntegrationResponseData;
import com.milesight.beaveriot.integration.service.IntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/integration")
public class IntegrationController {
    @Autowired
    IntegrationService integrationService;

    @PostMapping("/search")
    public ResponseBody<List<SearchIntegrationResponseData>> searchIntegration(@RequestBody SearchIntegrationRequest searchIntegrationRequest) {
        return ResponseBuilder.success(integrationService.searchIntegration(searchIntegrationRequest));
    }

    @GetMapping("/{integrationId}")
    public ResponseBody<IntegrationDetailData> getIntegrationDetail(@PathVariable("integrationId") String integrationId) {
        return ResponseBuilder.success(integrationService.getDetailData(integrationId));
    }
}
