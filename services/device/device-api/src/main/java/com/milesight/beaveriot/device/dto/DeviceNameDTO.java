package com.milesight.beaveriot.device.dto;

import com.milesight.beaveriot.context.integration.model.Integration;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeviceNameDTO {
    private Long id;
    private String key;
    private Long userId;
    private Integration integrationConfig;
    private String name;
    private Long createdAt;
}
