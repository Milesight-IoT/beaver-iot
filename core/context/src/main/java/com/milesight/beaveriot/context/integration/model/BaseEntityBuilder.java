package com.milesight.beaveriot.context.integration.model;

import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.enums.ValueStoreMod;
import com.milesight.beaveriot.context.integration.support.ValueStoreModSupport;
import com.milesight.beaveriot.context.support.IdentifierValidator;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.function.Supplier;

/**
 * @author leon
 */
public class BaseEntityBuilder<T extends BaseEntityBuilder<T>> {
    protected Long id;
    protected String name;

    protected String identifier;
    protected AccessMod accessMod;
    protected ValueStoreMod valueStoreMod;
    protected EntityType type;
    protected EntityValueType valueType;
    protected Map<String, Object> attributes;
    protected String parentIdentifier;

    protected Boolean visible = true;
    protected String description;

    public T identifier(String identifier) {
        IdentifierValidator.validate(identifier);
        this.identifier = identifier;
        return (T) this;
    }

    public T parentIdentifier(String parentIdentifier) {
        IdentifierValidator.validateNullable(parentIdentifier);
        this.parentIdentifier = parentIdentifier;
        return (T) this;
    }

    public T id(Long id) {
        this.id = id;
        return (T) this;
    }

    public T valueType(EntityValueType valueType) {
        this.valueType = valueType;
        return (T) this;
    }

    public T visible(Boolean visible) {
        this.visible = visible;
        return (T) this;
    }

    public T attributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        return (T) this;
    }

    public T attributes(Supplier<Map<String,Object>> attributeSupplier) {
        return this.attributes(attributeSupplier.get());
    }

    public T valueStoreMod(ValueStoreMod valueStoreMod) {
        this.valueStoreMod = valueStoreMod;
        return (T) this;
    }

    public T property(String name, AccessMod accessMod) {
        this.name = name;
        this.type = EntityType.PROPERTY;
        this.accessMod = accessMod;
        this.formatValueStoreMod();
        if (!StringUtils.hasLength(identifier)) {
            this.identifier = name;
        }
        return (T) this;
    }

    public T service(String name) {
        this.name = name;
        this.type = EntityType.SERVICE;
        this.accessMod = AccessMod.W;
        this.formatValueStoreMod();
        if (!StringUtils.hasLength(identifier)) {
            this.identifier = name;
        }
        return (T) this;
    }

    public T event(String name) {
        this.name = name;
        this.type = EntityType.EVENT;
        this.accessMod = AccessMod.W;
        this.formatValueStoreMod();
        if (!StringUtils.hasLength(identifier)) {
            this.identifier = name;
        }
        return (T) this;
    }

    private void formatValueStoreMod() {
        valueStoreMod = ValueStoreModSupport.format(type, valueStoreMod);
    }

    public T description(String description) {
        this.description = description;
        return (T) this;
    }

    protected Entity newInstance() {
        Entity entity = new Entity();
        entity.setId(id);
        entity.setName(name);
        entity.setIdentifier(identifier);
        entity.setAccessMod(accessMod);
        entity.setValueStoreMod(valueStoreMod);
        entity.setValueType(valueType);
        entity.setType(type);
        entity.setAttributes(attributes);
        entity.setParentIdentifier(parentIdentifier);
        entity.setVisible(visible);
        entity.setDescription(description);
        return entity;
    }

}
