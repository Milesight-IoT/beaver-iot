package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceLocation;

/**
 * author: Luxb
 * create: 2025/10/22 13:53
 **/
public interface DeviceLocationServiceProvider {
    DeviceLocation getLocation(Device device);
    void setLocation(Device device, DeviceLocation location);
    void clearLocation(Device device);
}
