package com.milesight.beaveriot.entity.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.entity.po.EntityLatestPO;
import com.milesight.beaveriot.permission.aspect.Tenant;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/16 15:33
 */
@Tenant
public interface EntityLatestRepository extends BaseJpaRepository<EntityLatestPO, Long> {
    List<EntityLatestPO> findByEntityIdIn(@Param("entityIds") List<Long> entityIds);
}
