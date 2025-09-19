package com.milesight.beaveriot.blueprint.library.repository;

import com.milesight.beaveriot.blueprint.library.po.BlueprintLibrarySubscriptionPO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/19 10:13
 **/
@Tenant
public interface BlueprintLibrarySubscriptionRepository extends BaseJpaRepository<BlueprintLibrarySubscriptionPO, Long> {
    @Tenant(enable = false)
    default List<BlueprintLibrarySubscriptionPO> findAllIgnoreTenant() {
        return findAll();
    }

    List<BlueprintLibrarySubscriptionPO> findAllByLibraryIdAndLibraryVersion(Long libraryId, String libraryVersion);

    List<BlueprintLibrarySubscriptionPO> findAllByLibraryId(Long libraryId);

    List<BlueprintLibrarySubscriptionPO> findAllByActiveTrue();

    @Modifying
    @Query("UPDATE BlueprintLibrarySubscriptionPO b " +
            "SET b.active = CASE " +
            "   WHEN b.libraryId = :libraryId THEN true " +
            "   ELSE false " +
            "END")
    void setActiveOnlyByLibraryId(
            @Param("libraryId") Long libraryId
    );
}