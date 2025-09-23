package com.milesight.beaveriot.entitytemplate.facade;

import com.milesight.beaveriot.context.integration.model.EntityTemplate;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/23 13:45
 **/
public interface IEntityTemplateFacade {
    void save(EntityTemplate entityTemplate);
    void batchSave(List<EntityTemplate> entityTemplates);
    List<EntityTemplate> findAll();
    List<EntityTemplate> findByKeys(List<String> keys);
    EntityTemplate findByKey(String key);
    void deleteByKey(String key);
    void deleteByKeys(List<String> keys);
}
