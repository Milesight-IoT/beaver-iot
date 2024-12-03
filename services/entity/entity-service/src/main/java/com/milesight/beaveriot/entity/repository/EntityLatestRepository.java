package com.milesight.beaveriot.entity.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.entity.po.EntityLatestPO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/16 15:33
 */
public interface EntityLatestRepository extends BaseJpaRepository<EntityLatestPO, Long> {

    @Modifying
    @Query(value = "delete from t_entity_latest d where d.entity_id in (?1)", nativeQuery = true)
    void deleteByEntityIds(List<Long> entityIds);

}
