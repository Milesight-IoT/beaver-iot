package com.milesight.beaveriot.device.status.service;

import com.milesight.beaveriot.context.api.DeviceStatusServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceStatus;
import com.milesight.beaveriot.context.integration.model.DeviceStatusConfig;
import org.springframework.stereotype.Service;

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
    public void register(String integrationId, DeviceStatusConfig config) {
        deviceStatusService.register(integrationId, config);
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
    public DeviceStatus status(Device device) {
        return deviceStatusService.status(device);
    }
}