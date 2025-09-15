package com.milesight.beaveriot.context.integration.model;

import com.milesight.beaveriot.base.enums.EntityErrorCode;
import com.milesight.beaveriot.base.error.ErrorHolder;
import com.milesight.beaveriot.base.utils.ValidationUtils;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.support.EntityValidator;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.eventbus.api.IdentityKey;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author leon
 */
@Getter
@Setter
public class Entity implements IdentityKey, Cloneable {

    private Long id;
    private String deviceKey;
    private String integrationId;
    private String name;
    private String identifier;
    private AccessMod accessMod;
    private EntityValueType valueType;
    private EntityType type;
    private Map<String, Object> attributes;
    private String parentIdentifier;
    private List<Entity> children;
    private Boolean visible;
    private String description;

    protected Entity() {
    }

    public String getFullIdentifier() {
        return StringUtils.hasLength(parentIdentifier) ? parentIdentifier + "." + identifier : identifier;
    }

    @Override
    public String getKey() {
        String fullIdentifier = getFullIdentifier();
        if (StringUtils.hasText(deviceKey)) {
            return IntegrationConstants.formatIntegrationDeviceEntityKey(deviceKey, fullIdentifier);
        } else {
            return IntegrationConstants.formatIntegrationEntityKey(integrationId, fullIdentifier);
        }
    }

    public String getParentKey() {
        if (!StringUtils.hasLength(parentIdentifier)) {
            return null;
        }
        if (StringUtils.hasText(deviceKey)) {
            return IntegrationConstants.formatIntegrationDeviceEntityKey(deviceKey, parentIdentifier);
        } else {
            return IntegrationConstants.formatIntegrationEntityKey(integrationId, parentIdentifier);
        }
    }

    public void initializeProperties(String integrationId, String deviceKey) {
        doValidate();
        Assert.notNull(integrationId, "Integration must not be null");
        Assert.notNull(deviceKey, "Device must not be null");
        this.setIntegrationId(integrationId);
        this.setDeviceKey(deviceKey);
        if (!CollectionUtils.isEmpty(children)) {
            children.forEach(entity -> {
                entity.setDeviceKey(deviceKey);
                entity.setIntegrationId(integrationId);
                entity.setParentIdentifier(identifier);
                applyParentConfig(entity);
            });
        }
    }

    private void applyParentConfig(Entity entity) {
        if (ObjectUtils.isEmpty(entity.getType())) {
            entity.setType(this.getType());
        }
        if (ObjectUtils.isEmpty(entity.getAccessMod())) {
            entity.setAccessMod(this.getAccessMod());
        }
        entity.setVisible(this.getVisible());
    }

    protected void initializeProperties(String integrationId) {
        doValidate();
        Assert.notNull(integrationId, "Integration must not be null");
        this.setIntegrationId(integrationId);
        if (!CollectionUtils.isEmpty(children)) {
            children.forEach(entity -> {
                entity.setIntegrationId(integrationId);
                entity.setParentIdentifier(identifier);
                applyParentConfig(entity);
            });
        }
    }

    public Optional<Device> loadDevice() {
        DeviceServiceProvider deviceServiceProvider = SpringContext.getBean(DeviceServiceProvider.class);
        return StringUtils.hasText(deviceKey) ? Optional.of(deviceServiceProvider.findByKey(deviceKey)) : Optional.empty();
    }

    public Optional<Integration> loadActiveIntegration() {
        IntegrationServiceProvider integrationServiceProvider = SpringContext.getBean(IntegrationServiceProvider.class);
        return Optional.ofNullable(integrationServiceProvider.getActiveIntegration(integrationId));
    }

    protected void doValidate() {
        Assert.notNull(identifier, "Entity identifier must not be null");
        Assert.notNull(type, "EntityType must not be null");
        Assert.notNull(valueType, "Entity ValueType must not be null");
        Assert.notNull(name, "Entity name must not be null");
        if(type == EntityType.PROPERTY){
            Assert.notNull(accessMod, "Entity AccessMod must not be null");
        }
    }

    public void validate() {
        doValidate();
        if (!CollectionUtils.isEmpty(getChildren())) {
            children.forEach(Entity::validate);
        }
    }

    public void setChildren(List<Entity> children) {
        this.children = children;
        if (children != null) {
            Assert.notNull(identifier, "Entity identifier must not be null");
            children.forEach(entity -> {
                entity.setParentIdentifier(identifier);
                if (deviceKey != null) {
                    entity.setDeviceKey(deviceKey);
                }
                if (integrationId != null) {
                    entity.setIntegrationId(integrationId);
                }
            });
        }
    }

    public boolean isOptional() {
        if (getAttributes() == null) {
            return false;
        }

        return Boolean.TRUE.equals(getAttributes().get(AttributeBuilder.ATTRIBUTE_OPTIONAL));
    }

    public void setImportant(Integer important) {
        if (important == null) {
            return;
        }

        if (getAttributes() == null) {
            setAttributes(new HashMap<>());
        }
        getAttributes().put(AttributeBuilder.ATTRIBUTE_IMPORTANT, important);
    }

    public Integer getImportant() {
        if (getAttributes() == null) {
            return null;
        }

        return (Integer) getAttributes().get(AttributeBuilder.ATTRIBUTE_IMPORTANT);
    }

    @Override
    public Entity clone() {
        Entity entity = new Entity();
        entity.setId(id);
        entity.setDeviceKey(deviceKey);
        entity.setIntegrationId(integrationId);
        entity.setName(name);
        entity.setIdentifier(identifier);
        entity.setAccessMod(accessMod);
        entity.setValueType(valueType);
        entity.setType(type);
        entity.setAttributes(attributes);
        entity.setParentIdentifier(parentIdentifier);
        entity.setChildren(children);
        entity.setVisible(visible);
        entity.setDescription(description);
        if (!ObjectUtils.isEmpty(children)) {
            List<Entity> copyChildren = children.stream().map(Entity::clone).collect(Collectors.toList());
            entity.setChildren(copyChildren);
        }
        return entity;
    }

    public Double getAttributeDoubleValue(String attributeKey) {
        return attributes == null ? null : (attributes.containsKey(attributeKey) ? Double.parseDouble(attributes.get(attributeKey).toString()) : null);
    }

    public Long getAttributeLongValue(String attributeKey) {
        return attributes == null ? null : (attributes.containsKey(attributeKey) ? Long.parseLong(attributes.get(attributeKey).toString()) : null);
    }

    public String getAttributeStringValue(String attributeKey) {
        return attributes == null ? null : (attributes.containsKey(attributeKey) ? attributes.get(attributeKey).toString() : null);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAttributeMapValue(String attributeKey) {
        return attributes == null ? null : (attributes.containsKey(attributeKey) ? (Map<String, Object>) attributes.get(attributeKey) : null);
    }

    public List<ErrorHolder> validateValue(Object value) {
        List<ErrorHolder> errors = new ArrayList<>();
        String entityKey = getKey();
        String entityName = getName();
        Map<String, Object> entityData = Map.of(ExtraDataConstants.KEY_ENTITY_KEY, entityKey, ExtraDataConstants.KEY_ENTITY_NAME, entityName);
        try {
            if (getParentKey() == null && EntityValueType.OBJECT.equals(valueType)) {
                return errors;
            }

            if (isOptional() && (value == null || value.toString().isEmpty())) {
                return errors;
            }

            if (!isOptional() && value == null) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_NULL.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_NULL.formatMessage(entityName), entityData));
                return errors;
            }

            if (!EntityValidator.isMatchType(valueType, value)) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_NOT_MATCH_TYPE.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_NOT_MATCH_TYPE.formatMessage(entityName, valueType.name(), value.getClass().getSimpleName()),
                        buildExtraData(entityData, Map.of(
                                ExtraDataConstants.KEY_REQUIRED_TYPE, valueType.name(),
                                ExtraDataConstants.KEY_PROVIDED_TYPE, value.getClass().getSimpleName()))));
                return errors;
            }

            if (EntityValueType.DOUBLE.equals(valueType) || EntityValueType.LONG.equals(valueType)) {
                validateValueRange(entityName, entityData, value, errors);
                validateValueFormat(entityName, entityData, value, errors);
            } else if (EntityValueType.STRING.equals(valueType)) {
                validateLengthRange(entityName, entityData, value, errors);
                validateValueEnum(entityName, entityData, value, errors);
                validateValueFormat(entityName, entityData, value, errors);
            }
        } catch (Exception e) {
            errors.clear();
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_VALIDATION_ERROR.getErrorCode(),
                    EntityErrorCode.ENTITY_VALUE_VALIDATION_ERROR.formatMessage(entityName, e.getMessage()),
                    entityData));
        }

        return errors;
    }

    private Map<String, Object> buildExtraData(Map<String, Object> baseData, Map<String, Object> specialData) {
        Map<String, Object> extraData = new HashMap<>(baseData);
        extraData.putAll(specialData);
        return extraData;
    }

    private static class ExtraDataConstants {
        public static final String KEY_ENTITY_KEY = "entity_key";
        public static final String KEY_ENTITY_NAME = "entity_name";
        public static final String KEY_REQUIRED_TYPE = "required_type";
        public static final String KEY_PROVIDED_TYPE = "provided_type";
    }

    private void validateValueRange(String entityName, Map<String, Object> entityData, Object value, List<ErrorHolder> errors) {
        Double min = getAttributeDoubleValue(AttributeBuilder.ATTRIBUTE_MIN);
        Double max = getAttributeDoubleValue(AttributeBuilder.ATTRIBUTE_MAX);
        double doubleValue = Double.parseDouble(value.toString());
        if (min != null && max != null) {
            if (doubleValue < min || doubleValue > max) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_OUT_OF_RANGE.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_OUT_OF_RANGE.formatMessage(entityName, min, max),
                        buildExtraData(entityData, Map.of(
                                AttributeBuilder.ATTRIBUTE_MIN, min,
                                AttributeBuilder.ATTRIBUTE_MAX, max
                        ))));
            }
        } else if (min != null) {
            if (doubleValue < min) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_LESS_THAN_MIN.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_LESS_THAN_MIN.formatMessage(entityName, min),
                        buildExtraData(entityData, Map.of(
                                AttributeBuilder.ATTRIBUTE_MIN, min
                        ))));
            }
        } else if (max != null){
            if (doubleValue > max) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_GREATER_THAN_MAX.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_GREATER_THAN_MAX.formatMessage(entityName, max),
                        buildExtraData(entityData, Map.of(
                                AttributeBuilder.ATTRIBUTE_MAX, max
                        ))));
            }
        }
    }

    private void validateLengthRange(String entityName, Map<String, Object> entityData, Object value, List<ErrorHolder> errors) {
        Long minLength = getAttributeLongValue(AttributeBuilder.ATTRIBUTE_MIN_LENGTH);
        Long maxLength = getAttributeLongValue(AttributeBuilder.ATTRIBUTE_MAX_LENGTH);
        long length = (value.toString()).length();
        if (minLength != null && maxLength != null) {
            if (length < minLength || length > maxLength) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_LENGTH_OUT_OF_RANGE.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_LENGTH_OUT_OF_RANGE.formatMessage(entityName,  minLength, maxLength),
                        buildExtraData(entityData, Map.of(
                                AttributeBuilder.ATTRIBUTE_MIN_LENGTH, minLength,
                                AttributeBuilder.ATTRIBUTE_MAX_LENGTH, maxLength
                        ))));
            }
        } else if (minLength != null) {
            if (length < minLength) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_LENGTH_SHORTER_THAN_MIN_LENGTH.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_LENGTH_SHORTER_THAN_MIN_LENGTH.formatMessage(entityName, minLength),
                        buildExtraData(entityData, Map.of(
                                AttributeBuilder.ATTRIBUTE_MIN_LENGTH, minLength
                        ))));
            }
        } else if (maxLength != null) {
            if (length > maxLength) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_LENGTH_LONGER_THAN_MAX_LENGTH.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_LENGTH_LONGER_THAN_MAX_LENGTH.formatMessage(entityName, maxLength),
                        buildExtraData(entityData, Map.of(
                                AttributeBuilder.ATTRIBUTE_MAX_LENGTH, maxLength
                        ))));
            }
        }

        String lengthRange = getAttributeStringValue(AttributeBuilder.ATTRIBUTE_LENGTH_RANGE);
        if (lengthRange != null) {
            String [] lengthRangeArray = lengthRange.split(",");
            boolean isLengthInRange = false;
            for (String lengthRangeItem : lengthRangeArray) {
                long enumLength = Long.parseLong(lengthRangeItem);
                if (length == enumLength) {
                    isLengthInRange = true;
                    break;
                }
            }
            if (!isLengthInRange) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_LENGTH_INVALID_ENUM.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_LENGTH_INVALID_ENUM.formatMessage(entityName, "{" + String.join(", ", lengthRangeArray) + "}"),
                        buildExtraData(entityData, Map.of(
                                AttributeBuilder.ATTRIBUTE_LENGTH_RANGE, lengthRange
                        ))));
            }
        }
    }

    private void validateValueEnum(String entityName, Map<String, Object> entityData, Object value, List<ErrorHolder> errors) {
        Map<String, Object> enumMap = getAttributeMapValue(AttributeBuilder.ATTRIBUTE_ENUM);
        if (CollectionUtils.isEmpty(enumMap)) {
            return;
        }

        if (!enumMap.containsKey(value.toString())) {
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_INVALID_ENUM.getErrorCode(),
                    EntityErrorCode.ENTITY_VALUE_INVALID_ENUM.formatMessage(entityName, "{" + String.join(", ", enumMap.keySet()) + "}"),
                    buildExtraData(entityData, Map.of(
                            AttributeBuilder.ATTRIBUTE_ENUM, enumMap
                    ))));
        }
    }

    private void validateValueFormat(String entityName, Map<String, Object> entityData, Object value, List<ErrorHolder> errors) {
        String format = getAttributeStringValue(AttributeBuilder.ATTRIBUTE_FORMAT);
        if (format == null) {
            return;
        }

        String stringValue = value.toString();
        boolean isValid = true;
        if (AttributeBuilder.ATTRIBUTE_FORMAT_VALUE_HEX.equals(format)) {
            isValid = ValidationUtils.isHex(stringValue);
        } else if (AttributeBuilder.ATTRIBUTE_FORMAT_VALUE_IMAGE.equals(format)) {
            isValid = ValidationUtils.isURL(stringValue) || ValidationUtils.isImageBase64(stringValue);
        } else if (AttributeBuilder.ATTRIBUTE_FORMAT_VALUE_IMAGE_URL.equals(format)) {
            isValid = ValidationUtils.isURL(stringValue);
        } else if (AttributeBuilder.ATTRIBUTE_FORMAT_VALUE_IMAGE_BASE64.equals(format)) {
            isValid = ValidationUtils.isImageBase64(stringValue);
        } else if (format.startsWith(AttributeBuilder.ATTRIBUTE_FORMAT_VALUE_REGEX)) {
            String [] regexArray = format.split(":");
            if (regexArray.length == 2) {
                String regex = regexArray[1];
                isValid = ValidationUtils.matches(stringValue, regex);
            }
        }

        if (!isValid) {
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_NOT_MATCH_FORMAT.getErrorCode(),
                    EntityErrorCode.ENTITY_VALUE_NOT_MATCH_FORMAT.formatMessage(entityName, format),
                    buildExtraData(entityData, Map.of(
                            AttributeBuilder.ATTRIBUTE_FORMAT, format
                    ))));
        }
    }
}
