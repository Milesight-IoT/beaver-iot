package com.milesight.beaveriot.user.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.user.po.RolePO;

/**
 * @author loong
 * @date 2024/11/19 17:51
 */
@Tenant
public interface RoleRepository extends BaseJpaRepository<RolePO, Long> {
}
