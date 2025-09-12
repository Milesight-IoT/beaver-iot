package com.milesight.beaveriot.device.service;

import com.milesight.beaveriot.context.api.DeviceBlueprintMappingServiceProvider;
import org.springframework.stereotype.Service;

/**
 * author: Luxb
 * create: 2025/9/9 14:56
 **/
@Service
public class DeviceBlueprintMappingServiceProviderImpl implements DeviceBlueprintMappingServiceProvider {
    private final DeviceBlueprintMappingService deviceBlueprintMappingService;

    public DeviceBlueprintMappingServiceProviderImpl(DeviceBlueprintMappingService deviceBlueprintMappingService) {
        this.deviceBlueprintMappingService = deviceBlueprintMappingService;
    }

    @Override
    public Long getBlueprintIdByDeviceId(Long deviceId) {
        return deviceBlueprintMappingService.getBlueprintIdByDeviceId(deviceId);
    }
}