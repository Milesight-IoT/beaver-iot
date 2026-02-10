package com.milesight.beaveriot.context.integration.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DeviceBasicData {
    private Long id;
    private String key;
    private String name;
    private String integrationId;
    private String identifier;
    private String template;
    private Map<String, Object> additionalData;
}
