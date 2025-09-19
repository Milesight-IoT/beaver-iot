package com.milesight.beaveriot.device.model.request;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import com.milesight.beaveriot.device.constants.DeviceDataFieldConstants;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * SearchDeviceGroupRequest class.
 *
 * @author simon
 * @date 2025/6/25
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SearchDeviceGroupRequest extends GenericPageRequest {
    @Size(max = DeviceDataFieldConstants.DEVICE_GROUP_NAME_MAX_LENGTH)
    private String name;

    private Boolean withDeviceCount;
}
