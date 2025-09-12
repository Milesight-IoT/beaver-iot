package com.milesight.beaveriot.context.api;

/**
 * author: Luxb
 * create: 2025/9/9 15:06
 **/
public interface DeviceBlueprintMappingServiceProvider {
    void saveMapping(Long deviceId, Long blueprintId);
    Long getBlueprintIdByDeviceId(Long deviceId);
    void deleteByDeviceId(Long deviceId);
}
