package com.milesight.beaveriot.devicetemplate.model.request;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import lombok.Data;

@Data
public class UpdateDeviceTemplateRequest {
    private String name;
    private String content;
    private String description;
    private String integration;
    private ExchangePayload paramEntities;
}
