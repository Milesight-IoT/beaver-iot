package com.milesight.beaveriot.resource.manager.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.resource.manager.model.request.RequestUploadConfig;
import com.milesight.beaveriot.resource.manager.service.ResourceService;
import com.milesight.beaveriot.resource.model.PreSignResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * ResourceController class.
 *
 * @author simon
 * @date 2025/4/1
 */
@Slf4j
@RestController
@RequestMapping("/resource")
public class ResourceController {

    @Autowired
    ResourceService resourceService;

    @PostMapping("/upload-config")
    public ResponseBody<PreSignResult> getResourceUploadConfig(@RequestBody RequestUploadConfig request) {
        return ResponseBuilder.success(resourceService.createPreSign(request));
    }
}
