package com.milesight.beaveriot.entity.repository;

import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.entity.po.EntityPO;
import com.milesight.beaveriot.permission.aspect.DataPermission;
import com.milesight.beaveriot.permission.enums.DataPermissionTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author loong
 * @date 2024/10/16 15:32
 */
public interface EntityRepository extends BaseJpaRepository<EntityPO, Long> {

    @Modifying
    @Query("delete from EntityPO d where d.attachTargetId = :targetId")
    void deleteByTargetId(@Param("targetId") String targetId);

    @DataPermission(type = DataPermissionTypeEnum.ENTITY, column = "id")
    default Optional<EntityPO> findOneWithDataPermission(Consumer<Filterable> filterable) {
        return findOne(filterable);
    }

    @DataPermission(type = DataPermissionTypeEnum.ENTITY, column = "id")
    default List<EntityPO> findAllWithDataPermission(Consumer<Filterable> filterable) {
        return findAll(filterable);
    }

    @DataPermission(type = DataPermissionTypeEnum.ENTITY, column = "id")
    default Page<EntityPO> findAllWithDataPermission(Consumer<Filterable> filterable, Pageable pageable) {
        return findAll(filterable, pageable);
    }

}
