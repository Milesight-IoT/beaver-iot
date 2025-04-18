package com.milesight.beaveriot.resource.manager.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.resource.manager.po.ResourcePO;

/**
 * ResourceRepository class.
 *
 * @author simon
 * @date 2025/4/14
 */
@Tenant
public interface ResourceRepository extends BaseJpaRepository<ResourcePO, Long> {
    ResourcePO findOneByUrl(String url);
}
