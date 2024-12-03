package com.milesight.beaveriot.dashboard.repository;

import com.milesight.beaveriot.dashboard.po.DashboardWidgetPO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * @author loong
 * @date 2024/10/14 17:14
 */
public interface DashboardWidgetRepository extends BaseJpaRepository<DashboardWidgetPO, Long> {

    @Modifying
    @Query(value = "delete from t_dashboard_widget d where d.dashboard_id = ?1", nativeQuery = true)
    void deleteByDashboardId(Long dashboardId);
}
