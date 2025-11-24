package com.milesight.beaveriot.entity.repository;

import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.entity.dto.EntityIdKeyDTO;
import com.milesight.beaveriot.entity.po.EntityPO;
import com.milesight.beaveriot.permission.aspect.DataPermission;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.permission.enums.ColumnDataType;
import com.milesight.beaveriot.permission.enums.DataPermissionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author loong
 * @date 2024/10/16 15:32
 */
@Tenant
public interface EntityRepository extends BaseJpaRepository<EntityPO, Long> {

    @DataPermission(type = DataPermissionType.ENTITY, column = "attach_target_id", dataType = ColumnDataType.STRING)
    default Optional<EntityPO> findOneWithDataPermission(Consumer<Filterable> filterable) {
        return findOne(filterable);
    }

    @DataPermission(type = DataPermissionType.ENTITY, column = "attach_target_id", dataType = ColumnDataType.STRING)
    default List<EntityPO> findAllWithDataPermission(Consumer<Filterable> filterable,
            Function<FluentQuery.FetchableFluentQuery<EntityPO>, List<EntityPO>> queryFunction) {
        return findBy(filterable, queryFunction);
    }

    @DataPermission(type = DataPermissionType.ENTITY, column = "attach_target_id", dataType = ColumnDataType.STRING)
    default List<EntityPO> findAllWithDataPermission(Consumer<Filterable> filterable) {
        return findAll(filterable);
    }

    @DataPermission(type = DataPermissionType.ENTITY, column = "attach_target_id", dataType = ColumnDataType.STRING)
    default Page<EntityPO> findAllWithDataPermission(Consumer<Filterable> filterable, Pageable pageable) {
        return findAll(filterable, pageable);
    }

    @Query("SELECT r.attachTargetId, COUNT(r) FROM EntityPO r WHERE r.attachTargetId IN :targetIds AND r.attachTarget = :targetType GROUP BY r.attachTargetId")
    List<Object[]> countAndGroupByTargets(@Param("targetType") AttachTargetType targetType,
            @Param("targetIds") List<String> targetIds);

    @Query("SELECT new com.milesight.beaveriot.entity.dto.EntityIdKeyDTO(e.id, e.key) FROM EntityPO e WHERE e.id IN :ids")
    List<EntityIdKeyDTO> findIdAndKeyByIdIn(@Param("ids") List<Long> ids);
}
