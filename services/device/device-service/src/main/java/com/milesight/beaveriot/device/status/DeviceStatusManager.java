package com.milesight.beaveriot.device.status;

import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceStatus;
import com.milesight.beaveriot.context.integration.model.DeviceStatusConfig;

/**
 * author: Luxb
 * create: 2025/9/4 8:55
 **/
public interface DeviceStatusManager {
    void register(String integrationId, DeviceStatusConfig config);
    void deregister(Device device);
    void online(Device device);
    void offline(Device device);
    DeviceStatus status(Device device);
}
