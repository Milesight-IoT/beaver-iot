package com.milesight.beaveriot.entity.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.entity.po.EntityTagMappingPO;
import com.milesight.beaveriot.permission.aspect.Tenant;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


@Tenant
public interface EntityTagMappingRepository extends BaseJpaRepository<EntityTagMappingPO, Long> {

    void deleteByTagIdIn(List<Long> tagIds);

    void deleteByEntityIdIn(List<Long> entityIds);

    void deleteByTagIdInAndEntityIdIn(List<Long> tagIds, List<Long> entityIds);

    List<EntityTagMappingPO> findByEntityIdIn(List<Long> entityIds);

    List<EntityTagMappingPO> findByTagIdInAndEntityIdIn(List<Long> tagIds, List<Long> entityIds);

    @Query(value = """
                    SELECT entity_id
                    FROM t_entity_tag_mapping m
                    LEFT JOIN t_entity_tag t ON t.id = m.tag_id
                    WHERE t.name IN :tagNames
                    GROUP BY entity_id
                    HAVING COUNT(DISTINCT tag_id) = :tagNumber;
            """, nativeQuery = true)
    List<Long> findEntityIdsByTagContains(@Param("tagNames") List<String> tagNames, @Param("tagNumber") Integer tagNumber);

    @Query(value = """
                    SELECT e.id AS entity_id
                    FROM t_entity e
                    LEFT JOIN t_entity_tag_mapping m ON e.id = m.entity_id
                    LEFT JOIN t_entity_tag t ON t.id = m.tag_id AND t.name IN :tagNames
                    GROUP BY e.id
                    HAVING COUNT(DISTINCT t.name) = 0;
            """, nativeQuery = true)
    List<Long> findEntityIdsByTagNotContains(@Param("tagNames") List<String> tagNames);

    @Query(value = """
                    SELECT entity_id
                    FROM t_entity_tag_mapping m
                    LEFT JOIN t_entity_tag t ON t.id = m.tag_id AND t.name IN :tagNames
                    GROUP BY entity_id
                    HAVING COUNT(DISTINCT tag_id) = COUNT(DISTINCT t.name);
            """, nativeQuery = true)
    List<Long> findEntityIdsByTagEquals(@Param("tagNames") List<String> tagNames);

    @Query(value = """
                    SELECT entity_id
                    FROM t_entity_tag_mapping m
                    LEFT JOIN t_entity_tag t ON t.id = m.tag_id
                    WHERE t.name IN :tagNames
                    GROUP BY entity_id
            """, nativeQuery = true)
    List<Long> findEntityIdsByTagIn(@Param("tagNames") List<String> tagNames);

    @Query(value = """
                    SELECT e.id AS entity_id
                    FROM t_entity e
                    LEFT JOIN t_entity_tag_mapping m ON e.id = m.entity_id
                    GROUP BY e.id
                    HAVING COUNT(DISTINCT tag_id) = 0;
            """, nativeQuery = true)
    List<Long> findEntityIdsByTagIsEmpty();

    @Query(value = """
                    SELECT entity_id
                    FROM t_entity_tag_mapping m
                    GROUP BY entity_id
                    HAVING COUNT(DISTINCT tag_id) > 0;
            """, nativeQuery = true)
    List<Long> findEntityIdsByTagIsNotEmpty();

}
