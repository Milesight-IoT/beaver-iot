package com.milesight.beaveriot.device.model.request;

import com.milesight.beaveriot.device.constants.DeviceDataFieldConstants;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

/**
 * author: Luxb
 * create: 2025/10/13 14:00
 **/
@Data
public class SetDeviceLocationRequest {
    @Size(max = DeviceDataFieldConstants.DEVICE_ADDRESS_MAX_LENGTH)
    private String address;

    @Range(min = -180, max = 180)
    private Double longitude;

    @Range(min = -90, max = 90)
    private Double latitude;
}
