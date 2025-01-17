package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.context.integration.model.Entity;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * @author leon
 */
public interface EntityServiceProvider {

    @NonNull
    List<Entity> findByTargetId(AttachTargetType targetType, String targetId);

    @NonNull
    List<Entity> findByTargetIds(AttachTargetType targetType, List<String> targetIds);

    void save(Entity entity);

    void batchSave(List<Entity> entityList);

    void deleteByTargetId(String targetId);

    Entity findByKey(String entityKey);

    Map<String, Entity> findByKeys(List<String> entityKeys);

    Entity findById(Long entityId);

    List<Entity> findByIds(List<Long> ids);

}
