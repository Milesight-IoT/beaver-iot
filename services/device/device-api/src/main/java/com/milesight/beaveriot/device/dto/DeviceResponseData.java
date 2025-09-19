package com.milesight.beaveriot.device.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
public class DeviceResponseData {
    private String id;
    private String key;
    private String name;
    private String integration;
    private String identifier;
    private Map<String, Object> additionalData;
    private String template;
    private Long createdAt;
    private Long updatedAt;

    private String integrationName;
    private String groupName;
    private String groupId;
    private Boolean deletable;
    private List<DeviceResponseEntityData> importantEntities;
}
