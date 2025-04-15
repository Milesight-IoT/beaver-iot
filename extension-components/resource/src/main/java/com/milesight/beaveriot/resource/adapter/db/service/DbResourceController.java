package com.milesight.beaveriot.resource.adapter.db.service;

import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.resource.adapter.db.service.po.DbResourceDataPO;
import jakarta.servlet.http.HttpServletResponse;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * DbResourceController class.
 *
 * @author simon
 * @date 2025/4/7
 */
@RestController
@RequestMapping(DbResourceConstants.RESOURCE_URL_PREFIX)
public class DbResourceController {
    @Autowired
    DbResourceService resourceService;

    private String getFullKey(String keyScope, String keyIdentifier) {
        return keyScope + "/" + keyIdentifier;
    }

    @PutMapping("/{keyScope}/{keyIdentifier}")
    public ResponseBody<Void> putResource(
            @PathVariable("keyScope") String keyScope,
            @PathVariable("keyIdentifier") String keyIdentifier,
            @RequestHeader("Content-Type") String contentType,
            @RequestBody byte[] fileData
    ) {
        resourceService.putResource(getFullKey(keyScope, keyIdentifier), contentType, fileData);
        return ResponseBuilder.success();
    }

    @GetMapping("/{keyScope}/{keyIdentifier}")
    public ResponseEntity<byte[]> putResource(
            @PathVariable("keyScope") String keyScope,
            @PathVariable("keyIdentifier") String keyIdentifier,
            HttpServletResponse response
    ) {
        DbResourceDataPO resourceDataPO = resourceService.getResource(getFullKey(keyScope, keyIdentifier));
        if (resourceDataPO == null) {
            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity
                .ok()
                .contentType(MediaType.valueOf(resourceDataPO.getContentType()))
                .body(resourceDataPO.getData());
    }
}
