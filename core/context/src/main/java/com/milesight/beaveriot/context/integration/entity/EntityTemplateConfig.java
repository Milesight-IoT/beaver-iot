package com.milesight.beaveriot.context.integration.entity;

import com.milesight.beaveriot.context.integration.model.EntityTemplate;
import lombok.Data;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/8/20 13:11
 **/
@Data
public class EntityTemplateConfig {
    public static final String PROPERTY_PREFIX = "entity-template";

    private List<EntityTemplate> initialEntityTemplates;
}
