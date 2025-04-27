package com.milesight.beaveriot.resource.adapter.db.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.resource.adapter.db.service.po.DbResourceDataPO;
import com.milesight.beaveriot.resource.config.ResourceConstants;
import jakarta.servlet.http.HttpServletResponse;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        if (fileData.length > ResourceConstants.MAX_FILE_SIZE) {
            throw ServiceException.with(ErrorCode.DATA_TOO_LARGE.getErrorCode(), "file too large").build();
        }

        resourceService.putResource(getFullKey(keyScope, keyIdentifier), contentType, fileData);
        return ResponseBuilder.success();
    }

    @GetMapping("/{keyScope}/{keyIdentifier}")
    public ResponseEntity<byte[]> getResource(
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
