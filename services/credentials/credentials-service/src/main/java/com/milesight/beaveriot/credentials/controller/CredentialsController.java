package com.milesight.beaveriot.credentials.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.credentials.model.request.AddCredentialsRequest;
import com.milesight.beaveriot.credentials.model.request.BatchDeleteCredentialsRequest;
import com.milesight.beaveriot.credentials.model.request.SearchCredentialsRequest;
import com.milesight.beaveriot.credentials.model.request.UpdateCredentialsRequest;
import com.milesight.beaveriot.credentials.model.response.CredentialsResponse;
import com.milesight.beaveriot.credentials.service.CredentialsService;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("/credentials")
public class CredentialsController {

    @Autowired
    private CredentialsService credentialsService;

    @GetMapping
    public ResponseBody<Page<CredentialsResponse>> searchCredentials(SearchCredentialsRequest request) {
        return ResponseBuilder.success(credentialsService.searchCredentials(request));
    }

    @GetMapping("/{id}")
    public ResponseBody<CredentialsResponse> getCredentials(@PathVariable("id") Long id) {
        return ResponseBuilder.success(credentialsService.getCredentialsResponse(id));
    }

    /**
     * Get default credentials
     *
     * @param credentialsType credentials type, e.g. MQTT, SMTP
     * @return credentials response
     */
    @GetMapping("/default/{credentials_type}")
    public ResponseBody<CredentialsResponse> getCredentials(@PathVariable("credentials_type") String credentialsType, @RequestParam(name = "auto_generate_password", required = false) Boolean autoGeneratePassword) {
        return ResponseBuilder.success(credentialsService.getOrCreateCredentialsResponse(credentialsType, autoGeneratePassword));
    }

    @PostMapping
    public ResponseBody<CredentialsResponse> addCredentials(@RequestBody AddCredentialsRequest request) {
        return ResponseBuilder.success(credentialsService.addCredentials(request));
    }

    @PutMapping("/{id}")
    public ResponseBody<CredentialsResponse> updateCredentials(@PathVariable("id") Long id, @RequestBody UpdateCredentialsRequest request) {
        return ResponseBuilder.success(credentialsService.updateCredentials(id, request));
    }

    @PostMapping("/delete")
    public ResponseBody<Void> batchDeleteCredentials(@RequestBody BatchDeleteCredentialsRequest request) {
        credentialsService.batchDeleteCredentials(request);
        return ResponseBuilder.success();
    }

}
