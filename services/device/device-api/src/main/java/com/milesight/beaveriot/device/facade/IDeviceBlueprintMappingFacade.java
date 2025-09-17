package com.milesight.beaveriot.device.facade;

/**
 * author: Luxb
 * create: 2025/9/12 13:06
 **/
public interface IDeviceBlueprintMappingFacade {
    void saveMapping(Long deviceId, Long blueprintId);
    Long getBlueprintIdByDeviceId(Long deviceId);
    Long getDeviceIdByBlueprintId(Long blueprintId);
    void deleteByDeviceId(Long deviceId);
}
