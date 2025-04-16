package com.milesight.beaveriot.resource.adapter.db.service.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.resource.adapter.db.service.model.DbResourceBasicProjection;
import com.milesight.beaveriot.resource.adapter.db.service.po.DbResourceDataPO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * DbResourceRepository class.
 *
 * @author simon
 * @date 2025/4/7
 */
public interface DbResourceDataRepository extends BaseJpaRepository<DbResourceDataPO, Long> {
    @Query("SELECT r.objKey AS objKey, r.contentType AS contentType, r.contentLength AS contentLength, r.createdAt AS createdAt FROM DbResourceDataPO r WHERE r.objKey IN :objKeys")
    List<DbResourceBasicProjection> findBasicByKeys(@Param("objKeys") List<String> objKeys);

    Optional<DbResourceDataPO> findByObjKey(String objKey);
}
