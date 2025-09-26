package com.milesight.beaveriot.device.service;

import com.milesight.beaveriot.context.api.DeviceBlueprintMappingServiceProvider;
import com.milesight.beaveriot.context.model.DeviceBlueprintMappingDTO;
import com.milesight.beaveriot.device.po.DeviceBlueprintMappingPO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
    public List<DeviceBlueprintMappingDTO> getBlueprintIdByDeviceIds(List<Long> deviceIdList) {
        List<DeviceBlueprintMappingPO> poList = deviceBlueprintMappingService.getBlueprintIdByDeviceIdList(deviceIdList);
        return poList.stream().map(po -> new DeviceBlueprintMappingDTO(po.getDeviceId(), po.getBlueprintId())).toList();
    }
}