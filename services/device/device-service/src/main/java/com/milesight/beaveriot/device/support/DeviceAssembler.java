package com.milesight.beaveriot.device.support;

import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/8/19 10:12
 **/
public abstract class DeviceAssembler {
    abstract List<EntityTemplate> getCommonEntityTemplates();

    public void assemble(Device device) {
        List<EntityTemplate> entityTemplates = getCommonEntityTemplates();
        if (!CollectionUtils.isEmpty(entityTemplates)) {
            List<Entity> commonEntities = entityTemplates.stream().map(entityTemplate -> entityTemplate.toEntity(device.getIntegrationId(), device.getKey())).toList();
            device.setEntities(merge(device.getEntities(), commonEntities));
        }
    }

    private List<Entity> merge(List<Entity> originEntities, List<Entity> commonEntities) {
        Map<String, Entity> entities = new HashMap<>();
        commonEntities.forEach(entity -> entities.put(entity.getFullIdentifier(), entity));
        List<Entity> deviceEntities = new ArrayList<>(commonEntities);
        originEntities.forEach(entity -> {
            if (!entities.containsKey(entity.getFullIdentifier())) {
                deviceEntities.add(entity);
                entities.put(entity.getFullIdentifier(), entity);
            }
        });
        return deviceEntities;
    }
}