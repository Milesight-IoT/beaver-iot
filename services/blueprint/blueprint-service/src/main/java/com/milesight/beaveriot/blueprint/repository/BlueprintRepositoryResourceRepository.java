package com.milesight.beaveriot.blueprint.repository;

import com.milesight.beaveriot.blueprint.po.BlueprintRepositoryResourcePO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;

/**
 * author: Luxb
 * create: 2025/9/1 9:40
 **/
public interface BlueprintRepositoryResourceRepository extends BaseJpaRepository<BlueprintRepositoryResourcePO, Long> {
    void deleteAllByRepositoryIdAndRepositoryVersion(Long repositoryId, String repositoryVersion);
    BlueprintRepositoryResourcePO findByRepositoryIdAndRepositoryVersionAndPath(Long repositoryId, String repositoryVersion, String resourcePath);
}
