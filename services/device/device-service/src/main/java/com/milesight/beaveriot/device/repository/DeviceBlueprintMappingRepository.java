package com.milesight.beaveriot.device.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.device.po.DeviceBlueprintMappingPO;
import com.milesight.beaveriot.permission.aspect.Tenant;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/9 14:54
 **/
@Tenant
public interface DeviceBlueprintMappingRepository extends BaseJpaRepository<DeviceBlueprintMappingPO, Long> {
    DeviceBlueprintMappingPO findByDeviceIdAndBlueprintId(Long deviceId, Long blueprintId);
    List<DeviceBlueprintMappingPO> findAllByDeviceId(Long deviceId);

    List<DeviceBlueprintMappingPO> findAllByDeviceIdIn(List<Long> deviceIdList);

    List<DeviceBlueprintMappingPO> findAllByBlueprintId(Long blueprintId);
    void deleteAllByDeviceId(Long deviceId);
}