package com.milesight.beaveriot.device.dto;

import com.milesight.beaveriot.context.integration.model.DeviceLocation;
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
    private String status;
    private DeviceLocation location;
    private String integrationName;
    private String groupName;
    private String groupId;
    private Boolean deletable;
    private List<DeviceResponseEntityData> importantEntities;
    private List<DeviceResponseEntityData> commonEntities;
}
