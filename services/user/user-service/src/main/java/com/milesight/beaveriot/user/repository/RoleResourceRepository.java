package com.milesight.beaveriot.user.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.user.po.RoleResourcePO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author loong
 * @date 2024/11/19 17:53
 */
@Tenant
public interface RoleResourceRepository extends BaseJpaRepository<RoleResourcePO, Long> {

    @Modifying
    @Query("delete from RoleResourcePO rp where rp.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Modifying
    @Query("delete from RoleResourcePO rp where rp.roleId = :roleId and rp.resourceType = :resourceType")
    void deleteByRoleIdAndResourceType(@Param("roleId") Long roleId, @Param("resourceType") String resourceType);

}
