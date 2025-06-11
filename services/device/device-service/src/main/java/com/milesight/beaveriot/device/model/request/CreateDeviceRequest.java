package com.milesight.beaveriot.device.model.request;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.device.constants.DeviceDataFieldConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDeviceRequest {
    @Size(max = DeviceDataFieldConstants.DEVICE_NAME_MAX_LENGTH)
    @NotBlank
    private String name;

    @NotBlank
    private String integration;

    private String template;

    private ExchangePayload paramEntities;
}
