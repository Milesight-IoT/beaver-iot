package com.milesight.beaveriot.dashboard.repository;

import com.milesight.beaveriot.dashboard.po.DashboardPO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.DataPermission;
import com.milesight.beaveriot.permission.enums.DataPermissionTypeEnum;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/14 17:13
 */
public interface DashboardRepository extends BaseJpaRepository<DashboardPO, Long> {

    @DataPermission(type = DataPermissionTypeEnum.DASHBOARD, column = "id")
    default List<DashboardPO> findAllWithDataPermission() {
        return findAll();
    }

}
