package com.milesight.beaveriot.user.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.user.po.UserPO;

/**
 * @author loong
 * @date 2024/10/14 10:55
 */
@Tenant
public interface UserRepository extends BaseJpaRepository<UserPO, Long> {
}
