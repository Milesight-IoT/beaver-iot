package com.milesight.beaveriot.device.repository;

import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.device.dto.DeviceIdKeyDTO;
import com.milesight.beaveriot.device.po.DevicePO;
import com.milesight.beaveriot.permission.aspect.DataPermission;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.permission.enums.DataPermissionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Tenant
public interface DeviceRepository extends BaseJpaRepository<DevicePO, Long> {
    List<DevicePO> findByIdIn(List<Long> ids);

    @Query("SELECT r.integration, COUNT(r) FROM DevicePO r WHERE r.integration IN :integrations GROUP BY r.integration")
    List<Object[]> countByIntegrations(@Param("integrations") List<String> integrations);

    @Tenant(enable = false)
    default Long countByTemplateInIgnoreTenant(List<String> templates) {
        return countByTemplateIn(templates);
    }

    Long countByTemplateIn(List<String> templates);

    @DataPermission(type = DataPermissionType.DEVICE, column = "id")
    default List<DevicePO> findAllWithDataPermission(Consumer<Filterable> filterable) {
        return findAll(filterable);
    }

    @DataPermission(type = DataPermissionType.DEVICE, column = "id")
    default Page<DevicePO> findAllWithDataPermission(Consumer<Filterable> filterable, Pageable pageable) {
        return findAll(filterable, pageable);
    }

    @DataPermission(type = DataPermissionType.DEVICE, column = "id")
    default Optional<DevicePO> findByIdWithDataPermission(Long id) {
        return findById(id);
    }

    @DataPermission(type = DataPermissionType.DEVICE, column = "id")
    default List<DevicePO> findByIdInWithDataPermission(List<Long> ids) {
        return findByIdIn(ids);
    }

    @DataPermission(type = DataPermissionType.DEVICE, column = "id")
    List<DevicePO> findAllByTemplate(String template);

    @Query("SELECT new com.milesight.beaveriot.device.dto.DeviceIdKeyDTO(d.id, d.key) FROM DevicePO d WHERE d.id IN :ids")
    List<DeviceIdKeyDTO> findIdAndKeyByIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT new com.milesight.beaveriot.device.dto.DeviceIdKeyDTO(d.id, d.key) FROM DevicePO d WHERE d.key IN :keys")
    List<DeviceIdKeyDTO> findIdAndKeyByKeyIn(@Param("keys") List<String> keys);
}
