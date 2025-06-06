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

public interface DeviceTemplateRepository extends BaseJpaRepository<DeviceTemplatePO, Long> {
    public List<DeviceTemplatePO> findByIdIn(List<Long> ids);

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
}
