package com.milesight.beaveriot.device.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.device.po.DeviceGroupMappingPO;
import com.milesight.beaveriot.permission.aspect.Tenant;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * DeviceGroupMappingRepository
 *
 * @author simon
 * @date 2025/6/25
 */
@Tenant
public interface DeviceGroupMappingRepository extends BaseJpaRepository<DeviceGroupMappingPO, Long> {
    void deleteAllByGroupId(Long groupId);

    List<DeviceGroupMappingPO> findAllByDeviceIdIn(List<Long> deviceId);

    @Query("SELECT DISTINCT(r.deviceId) FROM DeviceGroupMappingPO r")
    List<Long> findAllGroupedDeviceIdList();
}
