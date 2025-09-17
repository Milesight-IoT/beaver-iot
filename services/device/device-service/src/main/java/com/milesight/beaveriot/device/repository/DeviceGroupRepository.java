package com.milesight.beaveriot.device.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.device.po.DeviceGroupPO;
import com.milesight.beaveriot.permission.aspect.Tenant;

/**
 * DeviceGroupRepository
 *
 * @author simon
 * @date 2025/6/25
 */
@Tenant
public interface DeviceGroupRepository extends BaseJpaRepository<DeviceGroupPO, Long> {
}
