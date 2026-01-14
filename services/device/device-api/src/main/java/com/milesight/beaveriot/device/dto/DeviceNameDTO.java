package com.milesight.beaveriot.device.dto;

import jakarta.annotation.Nullable;
import lombok.*;

@Data
@Builder
public class DeviceNameDTO {
    private Long id;
    private String key;
    private String identifier;
    private Long userId;
    private String integrationId;
    @Nullable
    private String integrationName;
    private String template;
    private String name;
    private Long createdAt;

    public boolean isIntegrationExists() {
        return integrationName != null;
    }
}
