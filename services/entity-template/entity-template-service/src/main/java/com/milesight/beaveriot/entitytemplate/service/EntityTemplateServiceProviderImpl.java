package com.milesight.beaveriot.entitytemplate.service;

import com.milesight.beaveriot.context.api.EntityTemplateServiceProvider;
import com.milesight.beaveriot.context.integration.model.EntityTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/23 13:46
 **/
@Service
public class EntityTemplateServiceProviderImpl implements EntityTemplateServiceProvider {
    private final EntityTemplateService entityTemplateService;

    public EntityTemplateServiceProviderImpl(EntityTemplateService entityTemplateService) {
        this.entityTemplateService = entityTemplateService;
    }

    @Override
    public List<EntityTemplate> findAll() {
        return entityTemplateService.findAll();
    }

    @Override
    public List<EntityTemplate> findByKeys(List<String> keys) {
        return entityTemplateService.findByKeys(keys);
    }

    @Override
    public EntityTemplate findByKey(String key) {
        return entityTemplateService.findByKey(key);
    }
}
