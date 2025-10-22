package com.milesight.beaveriot.dashboard.repository;

import com.milesight.beaveriot.dashboard.po.DashboardHomePO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/14 17:13
 */
@Tenant
public interface DashboardHomeRepository extends BaseJpaRepository<DashboardHomePO, Long> {
    @Modifying
    void deleteByDashboardIdIn(List<Long> dashboardIdList);
}
