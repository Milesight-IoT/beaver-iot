package com.milesight.beaveriot.device.status;

import com.milesight.beaveriot.context.integration.model.Device;

import java.util.function.Function;

/**
 * author: Luxb
 * create: 2025/9/4 8:55
 **/
public interface DeviceStatusManager {
    void register(String integrationId, Function<Device, Long> offlineTimeoutFetcher);
    void deregister(Device device);
    void online(Device device);
    void offline(Device device);
    String status(Device device);
}
