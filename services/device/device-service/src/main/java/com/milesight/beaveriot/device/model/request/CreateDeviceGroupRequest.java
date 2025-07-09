package com.milesight.beaveriot.device.model.request;

import com.milesight.beaveriot.device.constants.DeviceDataFieldConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * CreateDeviceGroupRequest class.
 *
 * @author simon
 * @date 2025/6/25
 */
@Data
public class CreateDeviceGroupRequest {
    @Size(min = 1, max = DeviceDataFieldConstants.DEVICE_GROUP_NAME_MAX_LENGTH)
    @NotBlank
    private String name;
}
