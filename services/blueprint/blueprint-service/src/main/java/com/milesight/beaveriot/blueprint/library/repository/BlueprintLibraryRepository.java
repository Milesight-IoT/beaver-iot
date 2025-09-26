package com.milesight.beaveriot.blueprint.library.repository;

import com.milesight.beaveriot.blueprint.library.po.BlueprintLibraryPO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;

/**
 * author: Luxb
 * create: 2025/9/1 9:40
 **/
public interface BlueprintLibraryRepository extends BaseJpaRepository<BlueprintLibraryPO, Long> {
    BlueprintLibraryPO findByTypeAndUrlAndBranch(String type, String url, String branch);
}
