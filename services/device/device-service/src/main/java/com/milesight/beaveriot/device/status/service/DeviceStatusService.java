package com.milesight.beaveriot.device.status.service;

import com.milesight.beaveriot.context.api.DeviceStatusServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.device.status.DeviceStatusManager;
import org.springframework.stereotype.Service;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * author: Luxb
 * create: 2025/9/4 11:02
 **/
@Service
public class DeviceStatusService implements DeviceStatusServiceProvider {
    private final DeviceStatusManager deviceStatusManager;

    public DeviceStatusService(DeviceStatusManager deviceStatusManager) {
        this.deviceStatusManager = deviceStatusManager;
    }

    @Override
    public void register(String integrationId) {
        deviceStatusManager.register(integrationId);
    }

    @Override
    public void register(String integrationId, Function<Device, Long> offlineSecondsFetcher) {
        deviceStatusManager.register(integrationId, offlineSecondsFetcher);
    }

    @Override
    public void register(String integrationId, BiConsumer<Device, ExchangePayload> onlineUpdater, Consumer<Device> offlineUpdater, Function<Device, Long> offlineSecondsFetcher) {
        deviceStatusManager.register(integrationId, onlineUpdater, offlineUpdater, offlineSecondsFetcher);
    }

    @Override
    public void dataUploaded(Device device) {
        deviceStatusManager.dataUploaded(device);
    }

    @Override
    public void dataUploaded(Device device, ExchangePayload payload) {
        deviceStatusManager.dataUploaded(device, payload);
    }

    @Override
    public void updateDeviceStatusToOnline(Device device) {
        deviceStatusManager.updateDeviceStatusToOnline(device);
    }

    @Override
    public void updateDeviceStatusToOffline(Device device) {
        deviceStatusManager.updateDeviceStatusToOffline(device);
    }

    @Override
    public void deviceDeleted(Device device) {
        deviceStatusManager.deviceDeleted(device);
    }
}