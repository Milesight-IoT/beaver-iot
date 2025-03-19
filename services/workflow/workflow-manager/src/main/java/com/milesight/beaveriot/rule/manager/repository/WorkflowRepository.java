package com.milesight.beaveriot.rule.manager.repository;

import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.DataPermission;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.permission.enums.DataPermissionType;
import com.milesight.beaveriot.rule.manager.po.WorkflowPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Tenant
public interface WorkflowRepository extends BaseJpaRepository<WorkflowPO, Long> {
    public List<WorkflowPO> findByIdIn(List<Long> ids);

    // We do not control the data permission of workflow
    // @DataPermission(type = DataPermissionType.WORKFLOW, column = "id")
    default Optional<WorkflowPO> findByIdWithDataPermission(Long id) {
        return findById(id);
    }

    // @DataPermission(type = DataPermissionType.WORKFLOW, column = "id")
    default Page<WorkflowPO> findAllWithDataPermission(Consumer<Filterable> filterable, Pageable pageable){
        return findAll(filterable, pageable);
    }

    // @DataPermission(type = DataPermissionType.WORKFLOW, column = "id")
    default List<WorkflowPO> findByIdInWithDataPermission(List<Long> ids) {
        return findByIdIn(ids);
    }

    @Tenant(enable = false)
    default Page<WorkflowPO> findAllIgnoreTenant(Consumer<Filterable> filterable, Pageable pageable) {
        return findAll(filterable, pageable);
    }

}
