package com.milesight.beaveriot.user.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.user.po.MenuPO;

/**
 * @author loong
 * @date 2024/11/21 16:12
 */
@Tenant
public interface MenuRepository extends BaseJpaRepository<MenuPO, Long> {
}
