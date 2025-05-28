package com.milesight.beaveriot.dashboard.repository;

import com.milesight.beaveriot.dashboard.po.DashboardEntityPO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;


public interface DashboardEntityRepository extends BaseJpaRepository<DashboardEntityPO, Long> {

    @Modifying
    @Transactional
    void deleteAllByDashboardId(Long dashboardId);

}
