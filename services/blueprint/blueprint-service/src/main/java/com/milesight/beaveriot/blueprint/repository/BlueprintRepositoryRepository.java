package com.milesight.beaveriot.blueprint.repository;

import com.milesight.beaveriot.blueprint.po.BlueprintRepositoryPO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;

/**
 * author: Luxb
 * create: 2025/9/1 9:40
 **/
public interface BlueprintRepositoryRepository extends BaseJpaRepository<BlueprintRepositoryPO, Long> {
    BlueprintRepositoryPO findByHomeAndBranch(String home, String branch);
}
