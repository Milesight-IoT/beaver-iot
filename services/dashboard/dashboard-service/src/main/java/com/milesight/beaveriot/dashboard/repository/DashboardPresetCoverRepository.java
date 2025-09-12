package com.milesight.beaveriot.dashboard.repository;

import com.milesight.beaveriot.dashboard.po.DashboardPresetCoverPO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;

/**
 * DashboardPresetCoverRepository
 *
 * @author simon
 * @date 2025/9/9
 */
public interface DashboardPresetCoverRepository extends BaseJpaRepository<DashboardPresetCoverPO, Long> {
}
