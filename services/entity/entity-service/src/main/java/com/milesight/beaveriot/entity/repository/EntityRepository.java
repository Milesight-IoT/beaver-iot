package com.milesight.beaveriot.entity.repository;

import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.entity.po.EntityPO;
import com.milesight.beaveriot.permission.aspect.DataPermission;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.permission.enums.ColumnDataType;
import com.milesight.beaveriot.permission.enums.DataPermissionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.FluentQuery;

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
    default List<EntityPO> findAllWithDataPermission(Consumer<Filterable> filterable, Function<FluentQuery.FetchableFluentQuery<EntityPO>, List<EntityPO>> queryFunction) {
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

}
