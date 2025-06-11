package com.milesight.beaveriot.context.model.response;

import lombok.Data;

import java.util.Map;

@Data
public class DeviceTemplateResponseData {
    private String id;
    private String key;
    private String name;
    private String content;
    private String description;
    private String integration;
    private Map<String, Object> additionalData;
    private Long deviceCount;
    private Long createdAt;
    private Long updatedAt;

    private String integrationName;
    private Boolean deletable;
}
