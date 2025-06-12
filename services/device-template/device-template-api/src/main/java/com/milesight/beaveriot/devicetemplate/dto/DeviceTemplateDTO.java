package com.milesight.beaveriot.devicetemplate.dto;

import com.milesight.beaveriot.context.integration.model.Integration;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeviceTemplateDTO {
    private Long id;
    private String key;
    private Long userId;
    private String integrationId;
    private Integration integrationConfig;
    private String name;
    private String content;
    private String description;
    private Long createdAt;
}
