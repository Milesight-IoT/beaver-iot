package com.milesight.beaveriot.user.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.user.po.TenantPO;

/**
 * @author loong
 * @date 2024/11/19 17:51
 */
@Tenant(enable = false)
public interface TenantRepository extends BaseJpaRepository<TenantPO, Long> {
}
