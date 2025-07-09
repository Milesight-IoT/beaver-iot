package com.milesight.beaveriot.entity.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.entity.po.EntityTagMappingPO;
import com.milesight.beaveriot.permission.aspect.Tenant;

import java.util.List;


@Tenant
public interface EntityTagMappingRepository extends BaseJpaRepository<EntityTagMappingPO, Long> {

    void deleteByTagIdIn(List<Long> tagIds);

    void deleteByEntityIdIn(List<Long> entityIds);

    void deleteByTagIdInAndEntityIdIn(List<Long> tagIds, List<Long> entityIds);

    List<EntityTagMappingPO> findByEntityIdIn(List<Long> entityIds);

    List<EntityTagMappingPO> findByTagIdInAndEntityIdIn(List<Long> tagIds, List<Long> entityIds);

}
