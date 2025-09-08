package com.milesight.beaveriot.device.status;

import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * author: Luxb
 * create: 2025/9/4 8:55
 **/
public interface DeviceStatusManager {
    void register(String integrationId);
    void register(String integrationId, Function<Device, Long> offlineSecondsFetcher);
    void register(String integrationId, BiConsumer<Device, ExchangePayload> onlineUpdater, Consumer<Device> offlineUpdater, Function<Device, Long> offlineSecondsFetcher);
    void dataUploaded(Device device);
    void dataUploaded(Device device, ExchangePayload payload);
    void updateDeviceStatusToOnline(Device device);
    void updateDeviceStatusToOffline(Device device);
    void deviceDeleted(Device device);
}
