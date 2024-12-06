package com.milesight.beaveriot.entity.repository;

import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.entity.model.dto.EntityHistoryUnionQuery;
import com.milesight.beaveriot.entity.po.EntityHistoryPO;
import com.milesight.beaveriot.permission.aspect.DataPermission;
import com.milesight.beaveriot.permission.enums.DataPermissionTypeEnum;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author loong
 * @date 2024/10/16 15:32
 */
public interface EntityHistoryRepository extends BaseJpaRepository<EntityHistoryPO, Long> {

    @Modifying
    @org.springframework.data.jpa.repository.Query("delete from EntityHistoryPO d where d.entityId in :entityIds")
    void deleteByEntityIds(@Param("entityIds") List<Long> entityIds);

    @DataPermission(type = DataPermissionTypeEnum.ENTITY, column = "entity_id")
    default List<EntityHistoryPO> findAllWithDataPermission(Consumer<Filterable> consumer) {
        return findAll(consumer);
    }

    @DataPermission(type = DataPermissionTypeEnum.ENTITY, column = "entity_id")
    default Page<EntityHistoryPO> findAllWithDataPermission(Consumer<Filterable> filterable, Pageable pageable) {
        return findAll(filterable, pageable);
    }

    default List<EntityHistoryPO> findByUnionUnique(EntityManager entityManager, List<EntityHistoryUnionQuery> queries) {
        String dynamicQuery = generateDynamicQuery(queries);
        Query query = entityManager.createNativeQuery(dynamicQuery, EntityHistoryPO.class);

        Map<String, Object> parameters = generateParameters(queries);
        parameters.forEach(query::setParameter);

        return query.getResultList();
    }

    default String generateDynamicQuery(List<EntityHistoryUnionQuery> queries) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM t_entity_history WHERE ( ");
        for (int i = 0; i < queries.size(); i++) {
            if (i > 0) {
                sqlBuilder.append(" OR ");
            }
            sqlBuilder.append("(entity_id = :entityId").append(i).append(" AND timestamp = :timestamp").append(i).append(")");
        }
        sqlBuilder.append(" ) ");
        return sqlBuilder.toString();
    }

    default Map<String, Object> generateParameters(List<EntityHistoryUnionQuery> queries) {
        Map<String, Object> parameters = new HashMap<>();
        for (int i = 0; i < queries.size(); i++) {
            parameters.put("entityId" + i, queries.get(i).getEntityId());
            parameters.put("timestamp" + i, queries.get(i).getTimestamp());
        }
        return parameters;
    }

}
