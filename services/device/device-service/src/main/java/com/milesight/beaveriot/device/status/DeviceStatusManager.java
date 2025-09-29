package com.milesight.beaveriot.device.status;

import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceStatus;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * author: Luxb
 * create: 2025/9/4 8:55
 **/
public interface DeviceStatusManager {
    void register(String integrationId,
                  Consumer<Device> onlineListener,
                  Consumer<Device> offlineListener);
    void register(String integrationId,
                  Function<Device, Long> offlineTimeoutFetcher);
    void register(String integrationId,
                  Function<Device, Long> offlineTimeoutFetcher,
                  Consumer<Device> onlineListener,
                  Consumer<Device> offlineListener);
    void register(String integrationId,
                  Function<Device, Long> offlineTimeoutFetcher,
                  Function<List<Device>, Map<Long, Long>> batchOfflineTimeoutFetcher);
    void register(String integrationId,
                  Function<Device, Long> offlineTimeoutFetcher,
                  Function<List<Device>, Map<Long, Long>> batchOfflineTimeoutFetcher,
                  Consumer<Device> onlineListener,
                  Consumer<Device> offlineListener);
    void deregister(Device device);
    void online(Device device);
    void offline(Device device);
    DeviceStatus status(Device device);
}
