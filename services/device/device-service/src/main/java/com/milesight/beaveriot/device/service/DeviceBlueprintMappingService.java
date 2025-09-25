package com.milesight.beaveriot.device.service;

import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.device.facade.IDeviceBlueprintMappingFacade;
import com.milesight.beaveriot.device.po.DeviceBlueprintMappingPO;
import com.milesight.beaveriot.device.repository.DeviceBlueprintMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/9/9 14:56
 **/
@Service
public class DeviceBlueprintMappingService implements IDeviceBlueprintMappingFacade {
    private final DeviceBlueprintMappingRepository deviceBlueprintMappingRepository;

    public DeviceBlueprintMappingService(DeviceBlueprintMappingRepository deviceBlueprintMappingRepository) {
        this.deviceBlueprintMappingRepository = deviceBlueprintMappingRepository;
    }

    @Override
    public void saveMapping(Long deviceId, Long blueprintId) {
        DeviceBlueprintMappingPO mappingPO = deviceBlueprintMappingRepository.findByDeviceIdAndBlueprintId(deviceId, blueprintId);
        if (mappingPO == null) {
            mappingPO = new DeviceBlueprintMappingPO();
            mappingPO.setId(SnowflakeUtil.nextId());
            mappingPO.setDeviceId(deviceId);
            mappingPO.setBlueprintId(blueprintId);
            deviceBlueprintMappingRepository.save(mappingPO);
        }
    }

    @Override
    public Long getBlueprintIdByDeviceId(Long deviceId) {
        List<DeviceBlueprintMappingPO> mappingPOs = deviceBlueprintMappingRepository.findAllByDeviceId(deviceId);
        if (CollectionUtils.isEmpty(mappingPOs)) {
            return null;
        }

        return mappingPOs.get(0).getBlueprintId();
    }

    public List<DeviceBlueprintMappingPO> getBlueprintIdByDeviceIdList(List<Long> deviceIdList) {
        return deviceBlueprintMappingRepository.findAllByDeviceIdIn(deviceIdList);
    }

    @Override
    public Long getDeviceIdByBlueprintId(Long blueprintId) {
        List<DeviceBlueprintMappingPO> mappingPOs = deviceBlueprintMappingRepository.findAllByBlueprintId(blueprintId);
        if (CollectionUtils.isEmpty(mappingPOs)) {
            return null;
        }

        return mappingPOs.get(0).getDeviceId();
    }

    @Override
    public void deleteByDeviceId(Long deviceId) {
        deviceBlueprintMappingRepository.deleteAllByDeviceId(deviceId);
    }
}
