package com.milesight.beaveriot.context.integration.model.config;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.context.api.EntityTemplateServiceProvider;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.enums.ValueStoreMod;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.context.integration.model.EntityTemplate;
import com.milesight.beaveriot.context.support.SpringContext;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * @author leon
 */
@Getter
@Setter
public class EntityConfig {
    private static final EntityTemplateServiceProvider entityTemplateServiceProvider = SpringContext.getBean(EntityTemplateServiceProvider.class);
    private String name;
    private String identifier;
    private AccessMod accessMod;
    private ValueStoreMod valueStoreMod;
    private EntityValueType valueType;
    private EntityType type;
    private Map<String, Object> attributes;
    private List<Entity> children;
    private Boolean visible = true;
    private Integer important;
    private String entityRef;

    public Entity toEntity() {
        Entity entity;
        if (StringUtils.isEmpty(entityRef)) {
            EntityBuilder entityBuilder = new EntityBuilder();
            entityBuilder.valueStoreMod(valueStoreMod);
            switch (type) {
                case PROPERTY:
                    entityBuilder.property(name, accessMod);
                    break;
                case SERVICE:
                    entityBuilder.service(name);
                    break;
                case EVENT:
                    entityBuilder.event(name);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported entity type: " + type);
            }
            entity = entityBuilder.identifier(identifier)
                    .valueType(valueType)
                    .attributes(attributes)
                    .children(children)
                    .visible(visible)
                    .build();
        } else {
            EntityTemplate entityTemplate = entityTemplateServiceProvider.findByKey(entityRef);
            if (entityTemplate == null) {
                throw new IllegalArgumentException("Could not find entity key: '" + entityRef + "' in the entity template");
            }
            entity = entityTemplate.toEntity();
        }

        entity.setImportant(important);
        return entity;
    }
}
