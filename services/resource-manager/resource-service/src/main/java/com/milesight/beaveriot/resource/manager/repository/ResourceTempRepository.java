package com.milesight.beaveriot.resource.manager.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.resource.manager.po.ResourceTempPO;

/**
 * ResourceTempRepository
 *
 * @author simon
 * @date 2025/4/14
 */
public interface ResourceTempRepository extends BaseJpaRepository<ResourceTempPO, Long> {
    void deleteByResourceId(Long resourceId);
}
