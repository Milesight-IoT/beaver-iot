package com.milesight.beaveriot.device.model.request;

import com.milesight.beaveriot.device.location.model.DeviceLocationSetting;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * author: Luxb
 * create: 2025/10/13 14:00
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class SetDeviceLocationRequest extends DeviceLocationSetting {
}
