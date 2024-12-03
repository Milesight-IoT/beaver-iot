package com.milesight.beaveriot.context.integration.model.config;

import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
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

    private String name;
    private String identifier;
    private AccessMod accessMod;
    private EntityValueType valueType;
    private EntityType type;
    private Map<String, Object> attributes;
    private List<Entity> children;

    public Entity toEntity() {
        EntityBuilder entityBuilder = new EntityBuilder();
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
        return entityBuilder.identifier(identifier)
                .valueType(valueType)
                .attributes(attributes)
                .children(children)
                .build();
    }
}
