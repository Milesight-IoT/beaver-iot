package com.milesight.beaveriot.blueprint.core.repository;

import com.milesight.beaveriot.blueprint.core.po.BlueprintPO;
import com.milesight.beaveriot.permission.aspect.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Tenant
@Repository
public interface BlueprintRepository extends JpaRepository<BlueprintPO, Long> {

}
