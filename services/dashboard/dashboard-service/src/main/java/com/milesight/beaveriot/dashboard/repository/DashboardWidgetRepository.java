package com.milesight.beaveriot.dashboard.repository;

import com.milesight.beaveriot.dashboard.po.DashboardWidgetPO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author loong
 * @date 2024/10/14 17:14
 */
public interface DashboardWidgetRepository extends BaseJpaRepository<DashboardWidgetPO, Long> {

    @Modifying
    @Query("delete from DashboardWidgetPO d where d.dashboardId = :dashboardId")
    void deleteByDashboardId(@Param("dashboardId") Long dashboardId);
}
