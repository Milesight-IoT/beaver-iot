package com.milesight.beaveriot.device.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.device.po.DeviceGroupMappingPO;
import com.milesight.beaveriot.permission.aspect.DataPermission;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.permission.enums.DataPermissionType;
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

    /**
     * Find mapping with device data permission
     */
    @DataPermission(type = DataPermissionType.DEVICE, column = "device_id")
    List<DeviceGroupMappingPO> findAllByGroupIdIn(List<Long> groupId);

    @Query("SELECT DISTINCT(r.deviceId) FROM DeviceGroupMappingPO r")
    List<Long> findAllGroupedDeviceIdList();
}
