package com.milesight.beaveriot.context.integration.model;

import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.entity.annotation.AnnotationEntityCache;
import com.milesight.beaveriot.context.integration.entity.annotation.DeviceTemplateEntities;
import org.springframework.util.Assert;

import java.text.MessageFormat;
import java.util.List;

/**
 * @author leon
 */
public class AnnotatedTemplateEntityBuilder {

    private String integrationId;

    private String deviceIdentifier;

    public AnnotatedTemplateEntityBuilder(String integrationId, String deviceIdentifier) {
        this.integrationId = integrationId;
        this.deviceIdentifier = deviceIdentifier;
    }

    public List<Entity> build(Class<?> annotatedTemplateEntityClass) {

        Assert.notNull(integrationId, "Integration id must not be null");
        Assert.notNull(deviceIdentifier, "Device identifier must not be null");
        Assert.isTrue(annotatedTemplateEntityClass.isAnnotationPresent(DeviceTemplateEntities.class), "The class must be annotated with @DeviceTemplateEntities");

        List<Entity> deviceTemplateEntities = AnnotationEntityCache.INSTANCE.getDeviceTemplateEntities(annotatedTemplateEntityClass);
        return deviceTemplateEntities.stream().map(entity -> {
            Entity newEntity = entity.clone();
            String deviceKey = IntegrationConstants.formatIntegrationDeviceKey(integrationId, deviceIdentifier);
            newEntity.setIntegrationId(integrationId);
            newEntity.setDeviceKey(deviceKey);
            newEntity.getChildren().stream().forEach(child -> child.setDeviceKey(deviceKey));
            return newEntity;
        }).toList();
    }

}
