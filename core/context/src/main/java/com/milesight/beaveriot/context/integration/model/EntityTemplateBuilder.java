package com.milesight.beaveriot.context.integration.model;

import com.milesight.beaveriot.base.utils.MapUtils;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.enums.ValueStoreMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/8/20 15:04
 **/
public class EntityTemplateBuilder {
    private Long id;
    private String identifier;
    private String name;
    private EntityType type;
    private AccessMod accessMod;
    private ValueStoreMod valueStoreMod;
    private String parentIdentifier;
    private EntityValueType valueType;
    private Map<String, Object> attributes;
    private String description;
    private Boolean visible;
    private List<EntityTemplate> children;

    private EntityTemplateBuilder() {}

    public static EntityTemplateBuilder builder() {
        return new EntityTemplateBuilder();
    }

    public EntityTemplateBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public EntityTemplateBuilder identifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public EntityTemplateBuilder name(String name) {
        this.name = name;
        return this;
    }

    public EntityTemplateBuilder type(EntityType type) {
        this.type = type;
        return this;
    }

    public EntityTemplateBuilder accessMod(AccessMod accessMod) {
        this.accessMod = accessMod;
        return this;
    }

    public EntityTemplateBuilder valueStoreMod(ValueStoreMod valueStoreMod) {
        this.valueStoreMod = valueStoreMod;
        return this;
    }

    public EntityTemplateBuilder parentIdentifier(String parentIdentifier) {
        this.parentIdentifier = parentIdentifier;
        return this;
    }

    public EntityTemplateBuilder valueType(EntityValueType valueType) {
        this.valueType = valueType;
        return this;
    }

    public EntityTemplateBuilder attributes(Map<String, Object> attributes) {
        this.attributes = MapUtils.deepCopy(attributes);
        return this;
    }

    public EntityTemplateBuilder description(String description) {
        this.description = description;
        return this;
    }

    public EntityTemplateBuilder visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public EntityTemplateBuilder children(List<EntityTemplate> children) {
        this.children = children != null ? new ArrayList<>(children) : null;
        return this;
    }

    public EntityTemplate build() {
        EntityTemplate entityTemplate = new EntityTemplate();
        entityTemplate.setId(id);
        entityTemplate.setIdentifier(identifier);
        entityTemplate.setName(name);
        entityTemplate.setType(type);
        entityTemplate.setAccessMod(accessMod);
        entityTemplate.setValueStoreMod(valueStoreMod);
        entityTemplate.setParentIdentifier(parentIdentifier);
        entityTemplate.setValueType(valueType);
        entityTemplate.setAttributes(attributes);
        entityTemplate.setDescription(description);
        entityTemplate.formatValueStoreMod();
        if (visible != null) {
            entityTemplate.setVisible(visible);
        }
        entityTemplate.setChildren(children);
        return entityTemplate;
    }
}
