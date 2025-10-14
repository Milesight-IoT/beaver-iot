package com.milesight.beaveriot.blueprint.library.repository;

import com.milesight.beaveriot.blueprint.library.po.BlueprintLibraryVersionPO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/19 10:13
 **/
public interface BlueprintLibraryVersionRepository extends BaseJpaRepository<BlueprintLibraryVersionPO, Long> {
    List<BlueprintLibraryVersionPO> findAllByLibraryIdAndLibraryVersion(Long libraryId, String libraryVersion);
    List<BlueprintLibraryVersionPO> findAllByLibraryId(Long libraryId);
    void deleteByLibraryIdAndLibraryVersion(Long libraryId, String libraryVersion);
    Long countByLibraryId(Long libraryId);
}
