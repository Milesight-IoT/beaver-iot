package com.milesight.beaveriot.context.integration.entity.annotation;

import com.milesight.beaveriot.base.constants.StringConstant;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.entity.EntityLoader;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.AttributeBuilder;
import com.milesight.beaveriot.context.integration.model.DeviceBuilder;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.context.support.EnhancePropertySourcesPropertyResolver;
import com.milesight.beaveriot.context.support.PackagesScanner;
import jakarta.annotation.Nullable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author leon
 */
public class AnnotationEntityLoader implements EntityLoader {

    public static final String REPLACE_HOLDER_FIELD_NAME = "fieldName";

    private PackagesScanner packagesScanner;

    public AnnotationEntityLoader() {
        this.packagesScanner = new PackagesScanner();
    }

    @Override
    public void load(Integration integration, StandardEnvironment integrationEnvironment) {
        // scan annotation class
        EnhancePropertySourcesPropertyResolver propertyResolver = new EnhancePropertySourcesPropertyResolver(integrationEnvironment.getPropertySources());
        packagesScanner.doScan(integration.getIntegrationClass().getPackage().getName(), clazz -> {

            if (clazz.isAnnotationPresent(DeviceEntities.class)) {
                // parse DeviceEntities annotation
                DeviceEntities deviceEntitiesAnno = clazz.getAnnotation(DeviceEntities.class);
                DeviceBuilder deviceBuilder = new DeviceBuilder(integration.getId()).name(deviceEntitiesAnno.name()).identifier(deviceEntitiesAnno.identifier()).additional(resolveKeyValue(deviceEntitiesAnno.additional()));
                String deviceKey = IntegrationConstants.formatIntegrationDeviceKey(integration.getId(), deviceEntitiesAnno.identifier());
                List<com.milesight.beaveriot.context.integration.model.Entity> entities = parserEntities(integration, deviceKey, null, clazz, propertyResolver);
                deviceBuilder.entities(entities);
                integration.addInitialDevice(deviceBuilder.build());

            } else if (clazz.isAnnotationPresent(IntegrationEntities.class)) {
                // parse IntegrationEntities annotation
                List<com.milesight.beaveriot.context.integration.model.Entity> entities = parserEntities(integration, null, null, clazz, propertyResolver);
                integration.addInitialEntities(entities);

            } else if (clazz.isAnnotationPresent(DeviceTemplateEntities.class)) {
                String deviceKey = IntegrationConstants.formatIntegrationDeviceKey(integration.getId(), StringConstant.FORMAT_PLACE_HOLDER);
                List<com.milesight.beaveriot.context.integration.model.Entity> entities = parserEntities(integration, deviceKey, null, clazz, propertyResolver);
                AnnotationEntityCache.INSTANCE.cacheDeviceTemplateEntities(clazz, entities);
            }
        });
    }

    private List<com.milesight.beaveriot.context.integration.model.Entity> parserEntities(Integration integration, @Nullable String deviceKey, @Nullable String parentIdentifier, Class<?> clazz, EnhancePropertySourcesPropertyResolver propertyResolver) {
        List<com.milesight.beaveriot.context.integration.model.Entity> entities = new ArrayList<>();
        ReflectionUtils.doWithFields(clazz, field -> {
            Entity entityAnnotation = field.getAnnotation(Entity.class);
            if (entityAnnotation != null) {
                EntityValueType valueType = EntityValueType.of(field.getType());
                String name = propertyResolver.resolvePlaceholders(entityAnnotation.name(), field);
                String identifier = propertyResolver.resolvePlaceholders(entityAnnotation.identifier(), field);
                EntityBuilder entityBuilder = new EntityBuilder(integration.getId())
                        .identifier(identifier)
                        .attributes(resolveAttributes(entityAnnotation.attributes()))
                        .valueType(valueType)
                        .visible(entityAnnotation.visible())
                        .description(entityAnnotation.description())
                        .valueStoreMod(entityAnnotation.valueStoreMod());
                switch (entityAnnotation.type()) {
                    case EVENT:
                        entityBuilder.event(name);
                        break;
                    case SERVICE:
                        entityBuilder.service(name);
                        break;
                    case PROPERTY:
                        entityBuilder.property(name, entityAnnotation.accessMod());
                        break;
                    default:
                        break;
                }
                if (valueType == EntityValueType.OBJECT) {
                    List<com.milesight.beaveriot.context.integration.model.Entity> children = parserEntities(integration, deviceKey, identifier, field.getType(), propertyResolver);
                    children.forEach(entity -> {
                        entity.setType(entityAnnotation.type());
                        entity.setAccessMod(entityAnnotation.accessMod());
                        entity.setVisible(entityAnnotation.visible());
                    });
                    entityBuilder.children(children);
                }
                com.milesight.beaveriot.context.integration.model.Entity entity = entityBuilder.build();
                entity.setDeviceKey(deviceKey);
                entity.setParentIdentifier(parentIdentifier);
                entities.add(entity);
                AnnotationEntityCache.INSTANCE.cacheEntityMethod(field, entity.getKey());
            }
        });
        return entities;
    }

    private Map<String, Object> resolveAttributes(Attribute[] attributes) {
        if (ObjectUtils.isEmpty(attributes)) {
            return Map.of();
        }
        Attribute attribute = attributes[0];
        Class<? extends Enum> enumClass = ObjectUtils.isEmpty(attribute.enumClass()) ? null : attribute.enumClass()[0];
        return new AttributeBuilder()
                .unit(attribute.unit())
                .max(attribute.max())
                .min(attribute.min())
                .fractionDigits(attribute.fractionDigits())
                .maxLength(attribute.maxLength())
                .minLength(attribute.minLength())
                .format(attribute.format())
                .enums(enumClass)
                .optional(attribute.optional())
                .important(attribute.important())
                .defaultValue(attribute.defaultValue())
                .lengthRange(attribute.lengthRange())
                .build();
    }

    private Map<String, Object> resolveKeyValue(KeyValue[] keyValues) {
        return ObjectUtils.isEmpty(keyValues) ? Map.of() : Arrays.stream(keyValues).collect(Collectors.toMap(KeyValue::key, KeyValue::value));
    }

}
