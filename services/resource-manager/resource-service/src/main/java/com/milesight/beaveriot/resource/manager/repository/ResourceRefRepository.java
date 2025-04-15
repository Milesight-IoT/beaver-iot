package com.milesight.beaveriot.resource.manager.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.resource.manager.po.ResourceRefPO;

/**
 * ResourceRefRepository
 *
 * @author simon
 * @date 2025/4/14
 */
public interface ResourceRefRepository extends BaseJpaRepository<ResourceRefPO, Long> {
    void deleteByResourceId(Long resourceId);
}
