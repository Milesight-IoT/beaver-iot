package com.milesight.beaveriot.blueprint.core.repository;

import com.milesight.beaveriot.blueprint.core.po.BlueprintResourcePO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;


@Tenant
@Repository
public interface BlueprintResourceRepository extends BaseJpaRepository<BlueprintResourcePO, Long> {

    List<BlueprintResourcePO> findByBlueprintId(Long blueprintId);

    @Modifying
    void deleteAllByBlueprintId(Long blueprintId);

}
