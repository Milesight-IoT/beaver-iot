package com.milesight.beaveriot.context.integration.model;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 *
 * @author leon
 */
public class EntityBuilder extends BaseEntityBuilder<EntityBuilder> {

    private String integrationId;

    private String deviceKey;

    protected List<Entity> children = new ArrayList<>();

    public EntityBuilder() {
    }

    public EntityBuilder(String integrationId) {
        this.integrationId = integrationId;
    }

    public EntityBuilder(String integrationId, String deviceKey) {
        this.deviceKey = deviceKey;
        this.integrationId = integrationId;
    }

    public EntityBuilder children(Entity childrenEntity) {
        if(!ObjectUtils.isEmpty(childrenEntity)){
            children.add(childrenEntity);
        }
        return this;
    }

    public EntityBuilder children(List<Entity> childrenEntities) {
        if(!ObjectUtils.isEmpty(childrenEntities)){
            children.addAll(childrenEntities);
        }
        return this;
    }

    public EntityBuilder children(Supplier<List<Entity>> entitySupplier) {
        return children(entitySupplier.get());
    }

    public ChildEntityBuilder children() {
        return new ChildEntityBuilder(this);
    }

    public Entity build() {

        Entity entity = newInstance();
        entity.setChildren(children);
        if (StringUtils.hasText(integrationId)) {
            if (StringUtils.hasText(deviceKey)) {
                entity.initializeProperties(integrationId, deviceKey);
            } else {
                entity.initializeProperties(integrationId);
            }
        }
        entity.doValidate();
        return entity;
    }

    public static class ChildEntityBuilder extends BaseEntityBuilder<ChildEntityBuilder> {

        private EntityBuilder entityBuilder;

        public ChildEntityBuilder(EntityBuilder entityBuilder) {
            this.entityBuilder = entityBuilder;
            this.parentIdentifier = entityBuilder.identifier;
            this.visible = entityBuilder.visible;
        }

        public EntityBuilder end() {
            Entity entity = newInstance();
            entityBuilder.children(entity);
            return entityBuilder;
        }
    }
}
