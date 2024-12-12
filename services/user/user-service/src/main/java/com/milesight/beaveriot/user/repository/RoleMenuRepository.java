package com.milesight.beaveriot.user.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.user.po.RoleMenuPO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author loong
 * @date 2024/11/21 16:14
 */
@Tenant
public interface RoleMenuRepository extends BaseJpaRepository<RoleMenuPO, Long> {

    @Modifying
    @Query("delete from RoleMenuPO rm where rm.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Modifying
    @Query("delete from RoleMenuPO rm where rm.menuId = :menuId")
    void deleteByMenuId(@Param("menuId") Long menuId);

}
