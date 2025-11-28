package com.milesight.beaveriot.device.support;

import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityTemplateServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * author: Luxb
 * create: 2025/8/19 10:12
 **/
@Component
public abstract class DeviceAssembler {
    private final EntityTemplateServiceProvider entityTemplateServiceProvider;
    private final EntityServiceProvider entityServiceProvider;

    protected DeviceAssembler(EntityTemplateServiceProvider entityTemplateServiceProvider, EntityServiceProvider entityServiceProvider) {
        this.entityTemplateServiceProvider = entityTemplateServiceProvider;
        this.entityServiceProvider = entityServiceProvider;
    }

    abstract List<String> getCommonEntityKeys();

    public void assemble(Device device) {
        List<EntityTemplate> entityTemplates = getCommonEntityTemplates();
        if (!CollectionUtils.isEmpty(entityTemplates)) {
            List<Entity> commonEntities = entityTemplates.stream().map(entityTemplate -> entityTemplate.toEntity(device.getIntegrationId(), device.getKey())).toList();
            preserveCommonEntityNames(commonEntities);
            device.setEntities(merge(device.getEntities(), commonEntities));
        }
    }

    private void preserveCommonEntityNames(List<Entity> commonEntities) {
        if (CollectionUtils.isEmpty(commonEntities)) {
            return;
        }

        Map<String, Entity> commonEntityMap = new HashMap<>();
        commonEntities.forEach(entity -> {
            commonEntityMap.put(entity.getKey(), entity);
            if (!CollectionUtils.isEmpty(entity.getChildren())) {
                entity.getChildren().forEach(child -> commonEntityMap.put(child.getKey(), child));
            }
        });

        Map<String, Entity> existingEntityMap = entityServiceProvider.findByKeys(commonEntityMap.keySet());
        commonEntityMap.forEach((key, entity) -> {
            Entity existingEntity = existingEntityMap.get(key);
            if (existingEntity != null) {
                entity.setName(existingEntity.getName());
            }
        });
    }

    protected List<EntityTemplate> getCommonEntityTemplates() {
        List<String> commonEntityKeys = getCommonEntityKeys();
        if (CollectionUtils.isEmpty(commonEntityKeys)) {
            return null;
        }
        return entityTemplateServiceProvider.findByKeys(commonEntityKeys);
    }

    private List<Entity> merge(List<Entity> originEntities, List<Entity> commonEntities) {
        Map<String, Entity> entities = new HashMap<>();
        commonEntities.forEach(entity -> entities.put(entity.getFullIdentifier(), entity));
        List<Entity> deviceEntities = new ArrayList<>(commonEntities);
        if (originEntities != null) {
            originEntities.forEach(entity -> {
                if (!entities.containsKey(entity.getFullIdentifier())) {
                    deviceEntities.add(entity);
                    entities.put(entity.getFullIdentifier(), entity);
                }
            });
        }
        return deviceEntities;
    }
}