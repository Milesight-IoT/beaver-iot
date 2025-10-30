package com.milesight.beaveriot.device.location.service;

import com.milesight.beaveriot.context.api.DeviceLocationServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceLocation;
import org.springframework.stereotype.Service;

/**
 * author: Luxb
 * create: 2025/10/22 13:54
 **/
@Service
public class DeviceLocationServiceProviderImpl implements DeviceLocationServiceProvider {
    private final DeviceLocationService deviceLocationService;

    public DeviceLocationServiceProviderImpl(DeviceLocationService deviceLocationService) {
        this.deviceLocationService = deviceLocationService;
    }

    @Override
    public DeviceLocation getLocation(Device device) {
        return deviceLocationService.getLocation(device);
    }

    @Override
    public void setLocation(Device device, DeviceLocation location) {
        deviceLocationService.setLocation(device, location);
    }

    @Override
    public void clearLocation(Device device) {
        deviceLocationService.clearLocation(device);
    }
}
