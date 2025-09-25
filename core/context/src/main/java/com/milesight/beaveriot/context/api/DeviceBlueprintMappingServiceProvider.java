package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.model.DeviceBlueprintMappingDTO;

import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/9/9 15:06
 **/
public interface DeviceBlueprintMappingServiceProvider {
    List<DeviceBlueprintMappingDTO> getBlueprintIdByDeviceIds(List<Long> deviceIdList);
}