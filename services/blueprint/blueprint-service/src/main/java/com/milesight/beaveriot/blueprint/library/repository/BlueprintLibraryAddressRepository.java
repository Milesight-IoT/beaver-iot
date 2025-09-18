package com.milesight.beaveriot.blueprint.library.repository;

import com.milesight.beaveriot.blueprint.library.po.BlueprintLibraryAddressPO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/16 15:48
 **/
@Tenant
public interface BlueprintLibraryAddressRepository extends BaseJpaRepository<BlueprintLibraryAddressPO, Long> {
    @Tenant(enable = false)
    default List<BlueprintLibraryAddressPO> findAllIgnoreTenant() {
        return findAll();
    }

    @Tenant(enable = false)
    default List<BlueprintLibraryAddressPO> findAllByTypeAndUrlAndBranchIgnoreTenant(String type, String url, String branch) {
        return findAllByTypeAndUrlAndBranch(type, url, branch);
    }

    List<BlueprintLibraryAddressPO> findAllByActiveTrue();

    List<BlueprintLibraryAddressPO> findAllByTypeAndUrlAndBranch(String type, String url, String branch);

    @Modifying
    @Query("UPDATE BlueprintLibraryAddressPO b " +
            "SET b.active = CASE " +
            "   WHEN b.type = :type AND b.url = :url AND b.branch = :branch THEN true " +
            "   ELSE false " +
            "END")
    void setActiveOnlyByTypeUrlBranch(
            @Param("type") String type,
            @Param("url") String url,
            @Param("branch") String branch
    );

    @Modifying
    @Query("UPDATE BlueprintLibraryAddressPO b SET b.active = false")
    void setAllInactive();
}
