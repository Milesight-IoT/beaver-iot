package com.milesight.beaveriot.context.integration.model;

import com.milesight.beaveriot.base.enums.EntityErrorCode;
import com.milesight.beaveriot.base.error.ErrorHolder;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.base.utils.ValidationUtils;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.eventbus.api.IdentityKey;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        validate();
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
        validate();
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

    public void validate() {
        Assert.notNull(identifier, "Entity identifier must not be null");
        Assert.notNull(type, "EntityType must not be null");
        Assert.notNull(valueType, "Entity ValueType must not be null");
        Assert.notNull(name, "Entity name must not be null");
        if(type == EntityType.PROPERTY){
            Assert.notNull(accessMod, "Entity AccessMod must not be null");
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


    public Map<String, Object> getAttributeMapValue(String attributeKey) {
        return attributes == null ? null : (attributes.containsKey(attributeKey) ? JsonUtils.toMap(attributes.get(attributeKey)) : null);
    }

    public List<ErrorHolder> validate(Object value) {
        List<ErrorHolder> errors = new ArrayList<>();
        String noticeName = getKey();
        try {
            if (!isOptional() && value == null) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_NONE.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_NONE.formatMessage(noticeName)));
                return errors;
            }

            if (!isMatchType(value)) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_NOT_MATCH_TYPE.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_NOT_MATCH_TYPE.formatMessage(noticeName, valueType.name(), value.getClass().getSimpleName())));
                return errors;
            }

            if (EntityValueType.DOUBLE.equals(valueType) || EntityValueType.LONG.equals(valueType)) {
                validateValueRange(noticeName, value, errors);
                validateValueFormat(noticeName, value, errors);
            } else if (EntityValueType.STRING.equals(valueType)) {
                validateLengthRange(noticeName, value, errors);
                validateValueEnum(noticeName, value, errors);
                validateValueFormat(noticeName, value, errors);
            }
        } catch (Exception e) {
            errors.clear();
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_VALIDATE_ERROR.getErrorCode(),
                    EntityErrorCode.ENTITY_VALUE_VALIDATE_ERROR.formatMessage(noticeName, e.getMessage())));
        }

        return errors;
    }

    private void validateValueRange(String noticeName, Object value, List<ErrorHolder> errors) {
        Double min = getAttributeDoubleValue(AttributeBuilder.ATTRIBUTE_MIN);
        Double max = getAttributeDoubleValue(AttributeBuilder.ATTRIBUTE_MAX);
        double doubleValue = Double.parseDouble(value.toString());
        if (min != null && max != null) {
            if (doubleValue < min || doubleValue > max) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_OUT_OF_RANGE.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_OUT_OF_RANGE.formatMessage(noticeName, min, max)));
            }
        } else if (min != null) {
            if (doubleValue < min) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_LESS_THAN_MIN.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_LESS_THAN_MIN.formatMessage(noticeName, min)));
            }
        } else if (max != null){
            if (doubleValue > max) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_GRATER_THAN_MAX.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_GRATER_THAN_MAX.formatMessage(noticeName, max)));
            }
        }
    }

    private void validateLengthRange(String noticeName, Object value, List<ErrorHolder> errors) {
        Long minLength = getAttributeLongValue(AttributeBuilder.ATTRIBUTE_MIN_LENGTH);
        Long maxLength = getAttributeLongValue(AttributeBuilder.ATTRIBUTE_MAX_LENGTH);
        long length = (value.toString()).length();
        if (minLength != null && maxLength != null) {
            if (length < minLength || length > maxLength) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_LENGTH_OUT_OF_LENGTH_RANGE.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_LENGTH_OUT_OF_LENGTH_RANGE.formatMessage(noticeName,  minLength, maxLength)));
            }
        } else if (minLength != null) {
            if (length < minLength) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_LENGTH_SHORTER_THAN_MIN_LENGTH.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_LENGTH_SHORTER_THAN_MIN_LENGTH.formatMessage(noticeName, minLength)));
            }
        } else if (maxLength != null) {
            if (length > maxLength) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_LENGTH_LONGER_THAN_MAX_LENGTH.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_LENGTH_LONGER_THAN_MAX_LENGTH.formatMessage(noticeName, maxLength)));
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
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_LENGTH_OUT_OF_LENGTH_ENUM.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_LENGTH_OUT_OF_LENGTH_ENUM.formatMessage(noticeName, "{" + String.join(", ", lengthRangeArray) + "}")));
            }
        }
    }

    private void validateValueEnum(String noticeName, Object value, List<ErrorHolder> errors) {
        Map<String, Object> enumMap = getAttributeMapValue(AttributeBuilder.ATTRIBUTE_ENUM);
        if (CollectionUtils.isEmpty(enumMap)) {
            return;
        }

        if (!enumMap.containsKey(value.toString())) {
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_OUT_OF_ENUM.getErrorCode(),
                    EntityErrorCode.ENTITY_VALUE_OUT_OF_ENUM.formatMessage(noticeName, "{" + String.join(", ", enumMap.keySet()) + "}")));
        }
    }

    private void validateValueFormat(String noticeName, Object value, List<ErrorHolder> errors) {
        String format = getAttributeStringValue(AttributeBuilder.ATTRIBUTE_FORMAT);
        if (format == null) {
            return;
        }

        String stringValue = value.toString();
        boolean isValid = true;
        if (AttributeBuilder.ATTRIBUTE_FORMAT_VALUE_HEX.equals(format)) {
            isValid = ValidationUtils.isHex(stringValue);
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
                    EntityErrorCode.ENTITY_VALUE_NOT_MATCH_FORMAT.formatMessage(noticeName, format)));
        }
    }

    private boolean isMatchType(Object value) {
        if (value == null) {
            return false;
        }
        return switch (valueType) {
            case DOUBLE -> (value instanceof Float || value instanceof Double || value instanceof Integer || value instanceof Long);
            case LONG -> (value instanceof Integer || value instanceof Long);
            case BOOLEAN -> value instanceof Boolean;
            case STRING -> value instanceof String;
            case BINARY -> value instanceof byte[];
            case OBJECT -> value instanceof Map;
        };
    }
}
