package com.milesight.beaveriot.device.support;

import com.milesight.beaveriot.context.api.EntityTemplateServiceProvider;
import com.milesight.beaveriot.context.integration.model.EntityTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/8/19 10:12
 **/
@Component
public class GlobalDeviceAssembler extends DeviceAssembler {
    private final static List<String> COMMON_ENTITY_TEMPLATE_KEYS = List.of("location", "status");
    private final EntityTemplateServiceProvider entityTemplateServiceProvider;

    public GlobalDeviceAssembler(EntityTemplateServiceProvider entityTemplateServiceProvider) {
        this.entityTemplateServiceProvider = entityTemplateServiceProvider;
    }

    @Override
    List<EntityTemplate> getCommonEntityTemplates() {
        return entityTemplateServiceProvider.findByKeys(COMMON_ENTITY_TEMPLATE_KEYS);
    }
}
