package com.milesight.beaveriot.device.model.request;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import lombok.Data;

@Data
public class CreateDeviceRequest {
    private String name;

    private String integration;

    private ExchangePayload paramEntities;
}
