package com.milesight.beaveriot.device.model.request;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.device.constants.DeviceDataFieldConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
public class CreateDeviceRequest {
    @Size(max = DeviceDataFieldConstants.DEVICE_NAME_MAX_LENGTH)
    @NotBlank
    private String name;

    @NotBlank
    private String integration;

    private String template;

    @Size(max = DeviceDataFieldConstants.DEVICE_GROUP_NAME_MAX_LENGTH)
    private String groupName;

    @Size(max = DeviceDataFieldConstants.DEVICE_ADDRESS_MAX_LENGTH)
    private String address;

    @Range(min = -180, max = 180)
    private Double longitude;

    @Range(min = -90, max = 90)
    private Double latitude;

    private ExchangePayload paramEntities;
}
