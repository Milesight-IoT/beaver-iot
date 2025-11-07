package com.milesight.beaveriot.context.integration.model;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.enums.ValueStoreMod;
import com.milesight.beaveriot.context.integration.support.ValueStoreModSupport;
import com.milesight.beaveriot.context.support.function.SpELTemplateEvaluator;
import lombok.Data;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/8/19 10:13
 **/
@Data
public class EntityTemplate {
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
    private Boolean visible = true;
    private List<EntityTemplate> children;

    public String getKey() {
        return StringUtils.isEmpty(parentIdentifier) ? identifier : parentIdentifier + "." + identifier;
    }

    public String getParentKey() {
        return parentIdentifier;
    }

    public Entity toEntity() {
        return toEntity(null);
    }

    public Entity toEntity(String integrationId) {
        return toEntity(integrationId, null);
    }

    public Entity toEntity(String integrationId, String deviceKey) {
        return toEntity(null, integrationId, deviceKey);
    }

    public Entity toEntity(String name, String integrationId, String deviceKey) {
        return toEntity(name, null, integrationId, deviceKey);
    }

    public Entity toEntity(String name, String description, String integrationId, String deviceKey) {
        renderSelf();

        String entityName = StringUtils.isEmpty(name) ? this.name : name;
        String entityDescription = StringUtils.isEmpty(description) ? this.description : description;

        EntityBuilder entityBuilder = new EntityBuilder(integrationId, deviceKey);
        entityBuilder.identifier(identifier)
                .description(entityDescription)
                .valueType(valueType)
                .visible(visible)
                .attributes(attributes)
                .parentIdentifier(parentIdentifier)
                .valueStoreMod(valueStoreMod);

        switch (type) {
            case PROPERTY -> entityBuilder.property(entityName, accessMod);
            case SERVICE -> entityBuilder.service(entityName);
            case EVENT -> entityBuilder.event(entityName);
        }

        if (!CollectionUtils.isEmpty(children)) {
            entityBuilder.children(children.stream().map(child -> {
                child.applyParentConfig(this);
                return child.toEntity(integrationId, deviceKey);
            }).toList());
        }
        return entityBuilder.build();
    }

    @SuppressWarnings("unchecked")
    private void renderSelf() {
        SpELTemplateEvaluator evaluator = SpELTemplateEvaluator.getInstance();
        name = (String) evaluator.evaluate(name);
        description = (String) evaluator.evaluate(description);

        if (attributes != null) {
            Map<String, String> enums = (Map<String, String>) attributes.get(AttributeBuilder.ATTRIBUTE_ENUM);
            if (enums != null) {
                enums.forEach((key, value) -> {
                    String newValue = (String) evaluator.evaluate(value);
                    enums.put(key, newValue);
                });
            }

            String unit = (String) attributes.get(AttributeBuilder.ATTRIBUTE_UNIT);
            if (unit != null) {
                String newValue = (String) evaluator.evaluate(unit);
                attributes.put(AttributeBuilder.ATTRIBUTE_UNIT, newValue);
            }
        }
    }

    public void addChild(EntityTemplate child) {
        if (CollectionUtils.isEmpty(children)) {
            children = new ArrayList<>();
        }
        child.applyParentConfig(this);
        children.add(child);
    }

    public void initializeChildren() {
        formatValueStoreMod();
        if (!CollectionUtils.isEmpty(children)) {
            children.forEach(child -> child.applyParentConfig(this));
        }
    }

    public void applyParentConfig(EntityTemplate parent) {
        parentIdentifier = parent.getIdentifier();
        if (ObjectUtils.isEmpty(type)) {
            type = parent.getType();
        }
        if (ObjectUtils.isEmpty(accessMod)) {
            accessMod = parent.getAccessMod();
        }
        visible = parent.getVisible();
        this.formatValueStoreMod();
    }

    public void formatValueStoreMod() {
        valueStoreMod = ValueStoreModSupport.format(type, valueStoreMod);
    }
}