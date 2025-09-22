package com.milesight.beaveriot.device.status.service;

import com.milesight.beaveriot.context.api.DeviceStatusServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import org.springframework.stereotype.Service;

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
    public void register(String integrationId, Function<Device, Long> offlineTimeoutFetcher) {
        deviceStatusService.register(integrationId, offlineTimeoutFetcher);
    }

    @Override
    public void online(Device device) {
        deviceStatusService.online(device);
    }

    @Override
    public void offline(Device device) {
        deviceStatusService.offline(device);
    }

    @Override
    public String status(Device device) {
        return deviceStatusService.status(device);
    }
}