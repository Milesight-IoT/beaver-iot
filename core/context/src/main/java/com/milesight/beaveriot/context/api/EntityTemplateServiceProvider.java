package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.EntityTemplate;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/8/20 9:46
 **/
public interface EntityTemplateServiceProvider {
    void save(EntityTemplate entityTemplate);
    void batchSave(List<EntityTemplate> entityTemplates);
    List<EntityTemplate> findAll();
    List<EntityTemplate> findByKeys(List<String> keys);
    EntityTemplate findByKey(String key);
    void deleteByKey(String key);
    void deleteByKeys(List<String> keys);
}
