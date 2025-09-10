package com.milesight.beaveriot.blueprint.library.repository;

import com.milesight.beaveriot.blueprint.library.po.BlueprintLibraryResourcePO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;

/**
 * author: Luxb
 * create: 2025/9/1 9:40
 **/
public interface BlueprintLibraryResourceRepository extends BaseJpaRepository<BlueprintLibraryResourcePO, Long> {
    void deleteAllByLibraryIdAndLibraryVersion(Long libraryId, String libraryVersion);
    BlueprintLibraryResourcePO findByLibraryIdAndLibraryVersionAndPath(Long libraryId, String libraryVersion, String resourcePath);
}
