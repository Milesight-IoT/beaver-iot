package com.milesight.beaveriot.context.model.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * author: Luxb
 * create: 2025/6/9 14:58
 **/
@Data
public class DeviceTemplateDiscoverResponse {
    private List<EntityData> entities = new ArrayList<>();

    public void addEntity(String entityName, Object value) {
        EntityData entityData = new EntityData();
        entityData.setEntityName(entityName);
        entityData.setValue(value);
        entities.add(entityData);
    }

    @Data
    public static class EntityData {
        private String entityName;
        private Object value;
    }
}
