package com.milesight.beaveriot.device.dto;

import com.milesight.beaveriot.context.integration.model.Integration;
import lombok.*;

@Data
@Builder
public class DeviceNameDTO {
    private Long id;
    private String key;
    private Long userId;
    private String integrationId;
    private String template;
    private Integration integrationConfig;
    private String name;
    private Long groupId;
    private String groupName;
    private Long createdAt;
}
