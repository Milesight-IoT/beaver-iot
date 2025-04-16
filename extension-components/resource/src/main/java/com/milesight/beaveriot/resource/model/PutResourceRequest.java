package com.milesight.beaveriot.resource.model;

import lombok.Data;

import java.io.InputStream;

/**
 * PutResourceRequest class.
 *
 * @author simon
 * @date 2025/4/3
 */
@Data
public class PutResourceRequest {
    private String objectKey;

    private String contentType;

    private InputStream contentInput;

    private Long contentLength;
}
