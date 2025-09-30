package com.milesight.beaveriot.devicetemplate.repository;

import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.devicetemplate.po.DeviceTemplatePO;
import com.milesight.beaveriot.permission.aspect.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
@Tenant
public interface DeviceTemplateRepository extends BaseJpaRepository<DeviceTemplatePO, Long> {
    List<DeviceTemplatePO> findByIdIn(List<Long> ids);

    List<DeviceTemplatePO> findByBlueprintLibraryIdAndBlueprintLibraryVersionAndVendorAndModel(Long blueprintLibraryId, String blueprintLibraryVersion, String vendor, String model);

    @Tenant(enable = false)
    default List<DeviceTemplatePO> findByBlueprintLibraryIdAndBlueprintLibraryVersionIgnoreTenant(Long blueprintLibraryId, String blueprintLibraryVersion) {
        return findByBlueprintLibraryIdAndBlueprintLibraryVersion(blueprintLibraryId, blueprintLibraryVersion);
    }

    List<DeviceTemplatePO> findByBlueprintLibraryIdAndBlueprintLibraryVersion(Long blueprintLibraryId, String blueprintLibraryVersion);

    @Query("SELECT r.integration, COUNT(r) FROM DeviceTemplatePO r WHERE r.integration IN :integrations GROUP BY r.integration")
    List<Object[]> countByIntegrations(@Param("integrations") List<String> integrations);

    default Page<DeviceTemplatePO> findAllWithDataPermission(Consumer<Filterable> filterable, Pageable pageable){
        return findAll(filterable, pageable);
    }

    default Optional<DeviceTemplatePO> findByIdWithDataPermission(Long id) {
        return findById(id);
    }

    default List<DeviceTemplatePO> findByIdInWithDataPermission(List<Long> ids) {
        return findByIdIn(ids);
    }

    @Tenant(enable = false)
    default void deleteByIdInIgnoreTenant(List<Long> ids) {
        deleteByIdIn(ids);
    }

    void deleteByIdIn(List<Long> ids);
}
