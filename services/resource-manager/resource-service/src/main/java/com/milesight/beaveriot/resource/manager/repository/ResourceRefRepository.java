package com.milesight.beaveriot.resource.manager.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.resource.manager.po.ResourceRefPO;

import java.util.List;

/**
 * ResourceRefRepository
 *
 * @author simon
 * @date 2025/4/14
 */
public interface ResourceRefRepository extends BaseJpaRepository<ResourceRefPO, Long> {
    List<ResourceRefPO> findByRefIdAndRefType(String refId, String refType);

    List<ResourceRefPO> findByResourceIdIn(List<Long> resourceIds);
}
