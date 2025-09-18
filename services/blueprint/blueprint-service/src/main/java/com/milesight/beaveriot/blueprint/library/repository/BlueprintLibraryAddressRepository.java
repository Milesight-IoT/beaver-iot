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
    default List<BlueprintLibraryAddressPO> findAllByTypeAndHomeAndBranchIgnoreTenant(String type, String home, String branch) {
        return findAllByTypeAndHomeAndBranch(type, home, branch);
    }

    List<BlueprintLibraryAddressPO> findAllByActiveTrue();

    List<BlueprintLibraryAddressPO> findAllByTypeAndHomeAndBranch(String type, String home, String branch);

    @Modifying
    @Query("UPDATE BlueprintLibraryAddressPO b " +
            "SET b.active = CASE " +
            "   WHEN b.type = :type AND b.home = :home AND b.branch = :branch THEN true " +
            "   ELSE false " +
            "END")
    void setActiveOnlyByTypeHomeBranch(
            @Param("type") String type,
            @Param("home") String home,
            @Param("branch") String branch
    );

    @Modifying
    @Query("UPDATE BlueprintLibraryAddressPO b SET b.active = false")
    void setAllInactive();
}
