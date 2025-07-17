package com.milesight.beaveriot.resource.manager.model.request;

import lombok.Data;

/**
 * RequestUploadConfig class.
 *
 * @author simon
 * @date 2025/4/2
 */
@Data
public class RequestUploadConfig {
    private String name;

    private String fileName;

    private String description;

    private Integer tempResourceLiveMinutes;
}
