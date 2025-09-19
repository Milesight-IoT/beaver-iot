package com.milesight.beaveriot.dashboard.repository;

import com.milesight.beaveriot.dashboard.po.DashboardHomePO;
import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.DataPermission;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.permission.enums.DataPermissionType;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author loong
 * @date 2024/10/14 17:13
 */
@Tenant
public interface DashboardHomeRepository extends BaseJpaRepository<DashboardHomePO, Long> {

    @DataPermission(type = DataPermissionType.DASHBOARD, column = "dashboard_id")
    default List<DashboardHomePO> findAllWithDataPermission() {
        return findAll();
    }

    @DataPermission(type = DataPermissionType.DASHBOARD, column = "dashboard_id")
    default Optional<DashboardHomePO> findOneWithDataPermission(Consumer<Filterable> filterable) {
        return findOne(filterable);
    }

    @Modifying
    void deleteByDashboardIdIn(List<Long> dashboardIdList);
}
