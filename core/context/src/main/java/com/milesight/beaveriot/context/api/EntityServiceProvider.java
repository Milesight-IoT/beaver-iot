package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.context.integration.model.Entity;

import java.util.List;
import java.util.Map;

/**
 * @author leon
 */
public interface EntityServiceProvider {

    List<Entity> findByTargetId(AttachTargetType targetType, String targetId);

    List<Entity> findByTargetIds(AttachTargetType targetType, List<String> targetIds);

    void save(Entity entity);

    void batchSave(List<Entity> entityList);

    void deleteByTargetId(String targetId);

    long countAllEntitiesByIntegrationId(String integrationId);

    Map<String, Long> countAllEntitiesByIntegrationIds(List<String> integrationIds);

    long countIntegrationEntitiesByIntegrationId(String integrationId);

    Map<String, Long> countIntegrationEntitiesByIntegrationIds(List<String> integrationIds);

    Entity findByKey(String entityKey);

    Map<String, Entity> findByKeys(String... entityKeys);
}
