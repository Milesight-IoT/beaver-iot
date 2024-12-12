package com.milesight.beaveriot.user.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.user.po.UserRolePO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author loong
 * @date 2024/11/19 17:52
 */
@Tenant
public interface UserRoleRepository extends BaseJpaRepository<UserRolePO, Long> {

    @Modifying
    @Query("delete from UserRolePO ur where ur.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Modifying
    @Query("delete from UserRolePO ur where ur.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

}
