package com.milesight.beaveriot.resource.manager.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.resource.manager.po.ResourcePO;

import java.util.List;

/**
 * ResourceRepository class.
 *
 * @author simon
 * @date 2025/4/14
 */
@Tenant
public interface ResourceRepository extends BaseJpaRepository<ResourcePO, Long> {
    ResourcePO findOneByUrl(String url);

    @Tenant(enable = false)
    default List<ResourcePO> findAllByIdIgnoreTenant(Iterable<Long> ids) {
        return findAllById(ids);
    }

    @Tenant(enable = false)
    default void deleteAllIgnoreTenant(Iterable<ResourcePO> ids) {
        deleteAll(ids);
    }
}
