package com.milesight.beaveriot.device.status.service;

import com.milesight.beaveriot.context.api.DeviceStatusServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import org.springframework.stereotype.Service;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * author: Luxb
 * create: 2025/9/4 11:02
 **/
@Service
public class DeviceStatusServiceProviderImpl implements DeviceStatusServiceProvider {
    private final DeviceStatusService deviceStatusService;

    public DeviceStatusServiceProviderImpl(DeviceStatusService deviceStatusService) {
        this.deviceStatusService = deviceStatusService;
    }

    @Override
    public void register(String integrationId) {
        deviceStatusService.register(integrationId);
    }

    @Override
    public void register(String integrationId, Function<Device, Long> offlineSecondsFetcher) {
        deviceStatusService.register(integrationId, offlineSecondsFetcher);
    }

    @Override
    public void register(String integrationId, BiConsumer<Device, ExchangePayload> onlineUpdater, Consumer<Device> offlineUpdater, Function<Device, Long> offlineSecondsFetcher) {
        deviceStatusService.register(integrationId, onlineUpdater, offlineUpdater, offlineSecondsFetcher);
    }

    @Override
    public void dataUploaded(Device device) {
        deviceStatusService.dataUploaded(device);
    }

    @Override
    public void dataUploaded(Device device, ExchangePayload payload) {
        deviceStatusService.dataUploaded(device, payload);
    }

    @Override
    public void updateDeviceStatusToOnline(Device device) {
        deviceStatusService.updateDeviceStatusToOnline(device);
    }

    @Override
    public void updateDeviceStatusToOffline(Device device) {
        deviceStatusService.updateDeviceStatusToOffline(device);
    }
}