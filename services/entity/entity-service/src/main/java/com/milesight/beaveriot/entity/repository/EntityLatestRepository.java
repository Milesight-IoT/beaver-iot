package com.milesight.beaveriot.entity.repository;

import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.entity.po.EntityLatestPO;
import com.milesight.beaveriot.permission.aspect.DataPermission;
import com.milesight.beaveriot.permission.enums.DataPermissionTypeEnum;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author loong
 * @date 2024/10/16 15:33
 */
public interface EntityLatestRepository extends BaseJpaRepository<EntityLatestPO, Long> {

    @Modifying
    @Query("delete from EntityLatestPO d where d.entityId in :entityIds")
    void deleteByEntityIds(@Param("entityIds") List<Long> entityIds);

    @DataPermission(type = DataPermissionTypeEnum.ENTITY, column = "entity_id")
    default Optional<EntityLatestPO> fineOneWithDataPermission(Consumer<Filterable> filterable){
        return findOne(filterable);
    }

}
