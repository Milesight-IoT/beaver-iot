package com.milesight.beaveriot.dashboard.repository;

import com.milesight.beaveriot.dashboard.po.DashboardPO;
import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.DataPermission;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.permission.enums.DataPermissionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author loong
 * @date 2024/10/14 17:13
 */
@Tenant
public interface DashboardRepository extends BaseJpaRepository<DashboardPO, Long> {

    @DataPermission(type = DataPermissionType.DASHBOARD, column = "id")
    default List<DashboardPO> findAllWithDataPermission() {
        return findAll();
    }

    @DataPermission(type = DataPermissionType.DASHBOARD, column = "id")
    default List<DashboardPO> findWithDataPermission(Consumer<Filterable> filterable) {
        return findAll(filterable);
    }

    @DataPermission(type = DataPermissionType.DASHBOARD, column = "id")
    default Page<DashboardPO> findWithDataPermission(Consumer<Filterable> filterable, Pageable pageable) {
        return findAll(filterable, pageable);
    }

    @DataPermission(type = DataPermissionType.DASHBOARD, column = "id")
    default Optional<DashboardPO> findOneWithDataPermission(Consumer<Filterable> filterable) {
        return findOne(filterable);
    }

}
