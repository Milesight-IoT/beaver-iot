package com.milesight.beaveriot.devicetemplate.parser;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceBuilder;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.config.EntityConfig;
import com.milesight.beaveriot.devicetemplate.facade.IDeviceTemplateParserFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/5/15 13:22
 **/
abstract public class DeviceTemplateParser implements IDeviceTemplateParserFacade {
    @Autowired
    private DeviceServiceProvider deviceServiceProvider;
    @Autowired
    private EntityServiceProvider entityServiceProvider;
    abstract public boolean validate(String deviceTemplateContent);
    @Transactional(rollbackFor = Exception.class)
    abstract public void discover(String integration, Object data, Long deviceTemplateId, String deviceTemplateContent);

    protected Device saveDevice(String integration, String deviceId, String deviceName, Long deviceTemplateId) {
        Device device = new DeviceBuilder(integration)
                .name(deviceId)
                .templateId(deviceTemplateId)
                .identifier(deviceId)
                .additional(Map.of("deviceId", deviceId))
                .build();
        deviceServiceProvider.save(device);
        return device;
    }

    protected List<Entity> saveDeviceEntities(String integration, String deviceKey, List<EntityConfig> initialEntities) {
        if (CollectionUtils.isEmpty(initialEntities)) {
            return null;
        }
        List<Entity> entities = initialEntities.stream().map(entityConfig -> {
            Entity entity = entityConfig.toEntity();
            entity.setIntegrationId(integration);
            entity.setDeviceKey(deviceKey);
            return entity;
        }).toList();
        entities.forEach(entity -> entity.initializeProperties(integration, deviceKey));
        // test
        entities.forEach(entity -> {
            entity.setVisible(true);
            if (entity.getChildren() != null) {
                entity.getChildren().forEach(child -> {
                    child.setVisible(true);
                });
            }
        });
        entityServiceProvider.batchSave(entities);
        return entities;
    }
}
