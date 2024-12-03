package com.milesight.beaveriot.entity.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.entity.po.EntityPO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * @author loong
 * @date 2024/10/16 15:32
 */
public interface EntityRepository extends BaseJpaRepository<EntityPO, Long> {

    @Modifying
    @Query(value = "delete from t_entity d where d.attach_target_id = ?1", nativeQuery = true)
    void deleteByTargetId(String targetId);

}
