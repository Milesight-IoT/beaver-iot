package com.milesight.beaveriot.entity.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.entity.po.EntityTagPO;
import com.milesight.beaveriot.entity.po.EntityTagProjection;
import com.milesight.beaveriot.permission.aspect.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


@Tenant
public interface EntityTagRepository extends BaseJpaRepository<EntityTagPO, Long> {

    boolean existsByName(String name);

    @Query(value = """
                    SELECT t.*, COUNT(m.id) AS taggedEntitiesCount
                    FROM t_entity_tag t
                    LEFT JOIN t_entity_tag_mapping m ON t.id = m.tag_id
                    GROUP BY t.id
                    ORDER BY t.created_at DESC
            """, nativeQuery = true)
    Page<EntityTagProjection> search(Pageable pageable);

    @Query(value = """
                    SELECT t.*, COUNT(m.id) AS taggedEntitiesCount
                    FROM t_entity_tag t
                    LEFT JOIN t_entity_tag_mapping m ON t.id = m.tag_id
                    WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    GROUP BY t.id
                    ORDER BY t.created_at DESC
            """, nativeQuery = true)
    Page<EntityTagProjection> search(@Param("keyword") String keyword, Pageable pageable);

}
