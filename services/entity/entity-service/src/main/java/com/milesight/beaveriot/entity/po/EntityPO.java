package com.milesight.beaveriot.entity.po;

import com.milesight.beaveriot.base.enums.EntityErrorCode;
import com.milesight.beaveriot.base.error.ErrorHolderExt;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.base.utils.ValidationUtils;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.AttributeBuilder;
import com.milesight.beaveriot.data.support.MapJsonConverter;
import com.milesight.beaveriot.entity.constants.EntityDataFieldConstants;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author loong
 * @date 2024/10/16 14:25
 */
@Data
@Table(name = "t_entity")
@Entity
@FieldNameConstants
@EntityListeners(AuditingEntityListener.class)
@Slf4j
public class EntityPO {

    @Id
    private Long id;

    @Column(insertable = false, updatable = false)
    private String tenantId;

    private Long userId;

    @Column(name = "\"key\"", length = 512)
    private String key;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR")
    private EntityType type;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR")
    private AccessMod accessMod;

    @Column(length = 512)
    private String parent;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR")
    private AttachTargetType attachTarget;

    private String attachTargetId;

    @Convert(converter = MapJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> valueAttribute;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR")
    private EntityValueType valueType;

    /**
     * Whether the entity is visible to the user.
     */
    private Boolean visible = true;

    @CreatedDate
    private Long createdAt;

    @LastModifiedDate
    private Long updatedAt;

    @Column(columnDefinition = "TEXT")
    private String description;

    public boolean checkIsCustomizedEntity() {
        return IntegrationConstants.SYSTEM_INTEGRATION_ID.equals(attachTargetId);
    }

    private Integer getIntValue(Object input) {
        if (input instanceof Number num) {
            return num.intValue();
        } else if (input instanceof String str) {
            return Integer.parseInt(str);
        }

        return null;
    }

    private float getFloatValue(Object input) {
        if (input instanceof Number num) {
            return num.floatValue();
        } else if (input instanceof String str) {
            return Float.parseFloat(str);
        }

        return Float.NaN;
    }

    public boolean validateUserModifiedCustomEntity() {
        if (!checkIsCustomizedEntity()) {
            return true;
        }

        Map<String, Object> attribute = this.getValueAttribute();
        if (!EntityDataFieldConstants.CUSTOM_ENTITY_ALLOWED_ATTRIBUTES.containsAll(attribute.keySet())) {
            log.warn("Invalid attributes was passed {}.", attribute.keySet());
            return false;
        }

        Map<String, String> enums = (Map<String, String>) attribute.get(AttributeBuilder.ATTRIBUTE_ENUM);
        if (enums != null) {
            if (enums.size() > EntityDataFieldConstants.CUSTOM_ENTITY_ENUM_MAX_SIZE) {
                log.warn("Too many enums.");
                return false;
            }

            for (Map.Entry<String, String> entry : enums.entrySet()) {
                if (
                        entry.getKey().length() > EntityDataFieldConstants.CUSTOM_ENTITY_ENUM_STRING_MAX_LENGTH ||
                                entry.getValue().length() > EntityDataFieldConstants.CUSTOM_ENTITY_ENUM_STRING_MAX_LENGTH
                ) {
                    log.warn("Invalid enum string length");
                    return false;
                }
            }
        }

        Object isEnumAttr = attribute.get(EntityDataFieldConstants.CUSTOM_ENTITY_ATTRIBUTE_IS_ENUM);
        if (isEnumAttr != null && !(isEnumAttr instanceof Boolean)) {
            log.warn("Wrong type of isEnum attr");
            return false;
        }

        String unit = (String) attribute.get(AttributeBuilder.ATTRIBUTE_UNIT);
        if (unit != null && unit.length() > EntityDataFieldConstants.CUSTOM_ENTITY_UNIT_STRING_MAX_LENGTH) {
            log.warn("Unit too long.");
            return false;
        }

        for (String attrName : List.of(AttributeBuilder.ATTRIBUTE_MIN_LENGTH, AttributeBuilder.ATTRIBUTE_MAX_LENGTH)) {
            Object v = attribute.get(attrName);
            if (v == null) {
                continue;
            }

            Integer intValue = getIntValue(v);
            if (intValue == null || intValue < 0) {
                log.warn("MinLength or MaxLength Number error.");
                return false;
            }

            attribute.put(attrName, intValue);
        }

        for (String attrName : List.of(AttributeBuilder.ATTRIBUTE_MIN, AttributeBuilder.ATTRIBUTE_MAX)) {
            Object v = attribute.get(attrName);
            if (v == null) {
                continue;
            }

            float floatValue = getFloatValue(v);
            if (Float.isNaN(getFloatValue(floatValue))) {
                log.warn("MIN / MAX Number error.");
                return false;
            }

            attribute.put(attrName, floatValue);
        }

        return true;
    }

    public List<ErrorHolderExt> validate() {
        List<ErrorHolderExt> errors = new ArrayList<>();
        String entityKey = getKey();
        if (entityKey == null) {
            errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_KEY_NULL.getErrorCode(),
                    EntityErrorCode.ENTITY_KEY_NULL.getErrorMessage()));
            return errors;
        }

        try {
            if (type == null) {
                errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_TYPE_NULL.getErrorCode(),
                        EntityErrorCode.ENTITY_TYPE_NULL.formatMessage(entityKey)));
            } else {
                if (type.equals(EntityType.PROPERTY) && accessMod == null) {
                    errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_ACCESS_MOD_NULL.getErrorCode(),
                            EntityErrorCode.ENTITY_ACCESS_MOD_NULL.formatMessage(entityKey)));
                }
            }

            if (valueType == null) {
                errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_VALUE_TYPE_NULL.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_TYPE_NULL.formatMessage(entityKey)));
            }

            if (StringUtils.isEmpty(name)) {
                errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_NAME_EMPTY.getErrorCode(),
                        EntityErrorCode.ENTITY_NAME_EMPTY.formatMessage(entityKey)));
            }

            if (attachTarget == null) {
                errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_ATTACH_TARGET_NULL.getErrorCode(),
                        EntityErrorCode.ENTITY_ATTACH_TARGET_NULL.formatMessage(entityKey)));
            }

            if (attachTargetId == null) {
                errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_ATTACH_TARGET_ID_NULL.getErrorCode(),
                        EntityErrorCode.ENTITY_ATTACH_TARGET_ID_NULL.formatMessage(entityKey)));
            }

            if (valueAttribute != null) {
                validateAttributeMinAndMax(entityKey, errors);
                validateAttributeMinLengthAndMaxLength(entityKey, errors);
                validateAttributeLengthRange(entityKey, errors);
                validateAttributeFractionDigits(entityKey, errors);
                validateAttributeDefaultValue(entityKey, errors);
                validateAttributeOptional(entityKey, errors);
                validateAttributeEnum(entityKey, errors);
            }
        } catch (Exception e) {
            errors.clear();
            errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_VALUE_VALIDATION_ERROR.getErrorCode(),
                    EntityErrorCode.ENTITY_VALUE_VALIDATION_ERROR.formatMessage(entityKey, e.getMessage())));
        }
        return errors;
    }

    private void validateAttributeEnum(String entityKey, List<ErrorHolderExt> errors) {
        Object enums = valueAttribute.get(AttributeBuilder.ATTRIBUTE_ENUM);
        if (enums == null) {
            return;
        }

        if (!(enums instanceof Map)) {
            errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_ATTRIBUTE_ENUM_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_ENUM_INVALID.formatMessage(entityKey)));
        }
    }

    private void validateAttributeOptional(String entityKey, List<ErrorHolderExt> errors) {
        Object optional = valueAttribute.get(AttributeBuilder.ATTRIBUTE_OPTIONAL);
        if (optional == null) {
            return;
        }

        if (!(optional instanceof Boolean)) {
            errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_ATTRIBUTE_OPTIONAL_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_OPTIONAL_INVALID.formatMessage(entityKey)));
        }
    }

    private void validateAttributeDefaultValue(String entityKey, List<ErrorHolderExt> errors) {
        Object defaultValue = valueAttribute.get(AttributeBuilder.ATTRIBUTE_DEFAULT_VALUE);
        if (defaultValue == null) {
            return;
        }

        if (!isMatchType(defaultValue)) {
            errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_ATTRIBUTE_DEFAULT_VALUE_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_DEFAULT_VALUE_INVALID.formatMessage(entityKey, valueType.name())));
        }
    }

    private boolean isMatchType(Object value) {
        return switch (valueType) {
            case DOUBLE -> ValidationUtils.isNumber(value.toString());
            case LONG -> ValidationUtils.isInteger(value.toString());
            case BOOLEAN -> value instanceof Boolean;
            case STRING -> value instanceof String;
            case BINARY -> value instanceof byte[];
            default -> true;
        };
    }

    private void validateAttributeFractionDigits(String entityKey, List<ErrorHolderExt> errors) {
        Object fractionDigits = valueAttribute.get(AttributeBuilder.ATTRIBUTE_FRACTION_DIGITS);
        if (fractionDigits == null) {
            return;
        }

        if (!ValidationUtils.isPositiveInteger(fractionDigits.toString())) {
            errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_ATTRIBUTE_FRACTION_DIGITS_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_FRACTION_DIGITS_INVALID.formatMessage(entityKey)));
        }
    }

    private void validateAttributeLengthRange(String entityKey, List<ErrorHolderExt> errors) {
        Object lengthRange = valueAttribute.get(AttributeBuilder.ATTRIBUTE_LENGTH_RANGE);
        if (lengthRange == null) {
            return;
        }

        if (!(lengthRange instanceof String)) {
            errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_ATTRIBUTE_LENGTH_RANGE_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_LENGTH_RANGE_INVALID.formatMessage(entityKey)));
            return;
        }

        String[] lengthRangeArray = lengthRange.toString().split(",");
        for (String lengthRangeItem : lengthRangeArray) {
            if (!ValidationUtils.isPositiveInteger(lengthRangeItem)) {
                errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_ATTRIBUTE_LENGTH_RANGE_INVALID.getErrorCode(),
                        EntityErrorCode.ENTITY_ATTRIBUTE_LENGTH_RANGE_INVALID.formatMessage(entityKey)));
                return;
            }
        }
    }

    private void validateAttributeMinLengthAndMaxLength(String entityKey, List<ErrorHolderExt> errors) {
        boolean isMinLengthValid = true;
        Object minLength = valueAttribute.get(AttributeBuilder.ATTRIBUTE_MIN_LENGTH);
        if (minLength != null && !ValidationUtils.isPositiveInteger(minLength.toString())) {
            isMinLengthValid = false;
            errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_ATTRIBUTE_MIN_LENGTH_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_MIN_LENGTH_INVALID.formatMessage(entityKey)));
        }

        boolean isMaxLengthValid = true;
        Object maxLength = valueAttribute.get(AttributeBuilder.ATTRIBUTE_MAX_LENGTH);
        if (maxLength != null && !ValidationUtils.isPositiveInteger(maxLength.toString())) {
            isMaxLengthValid = false;
            errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_ATTRIBUTE_MAX_LENGTH_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_MAX_LENGTH_INVALID.formatMessage(entityKey)));
        }

        if (minLength != null && maxLength != null && isMinLengthValid && isMaxLengthValid) {
            if (Integer.parseInt(minLength.toString()) > Integer.parseInt(maxLength.toString())) {
                errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_ATTRIBUTE_MIN_LENGTH_GREATER_THAN_MAX_LENGTH.getErrorCode(),
                        EntityErrorCode.ENTITY_ATTRIBUTE_MIN_LENGTH_GREATER_THAN_MAX_LENGTH.formatMessage(entityKey, minLength, maxLength)));
            }
        }
    }

    private void validateAttributeMinAndMax(String entityKey, List<ErrorHolderExt> errors) {
        boolean isMinValid = true;
        Object min = valueAttribute.get(AttributeBuilder.ATTRIBUTE_MIN);
        if (min != null && !ValidationUtils.isNumber(min.toString())) {
            isMinValid = false;
            errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_ATTRIBUTE_MIN_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_MIN_INVALID.formatMessage(entityKey)));
        }

        boolean isMaxValid = true;
        Object max = valueAttribute.get(AttributeBuilder.ATTRIBUTE_MAX);
        if (max != null && !ValidationUtils.isNumber(max.toString())) {
            isMaxValid = false;
            errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_ATTRIBUTE_MAX_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_MAX_INVALID.formatMessage(entityKey)));
        }

        if (min != null && max != null && isMinValid && isMaxValid) {
            if (Double.parseDouble(min.toString()) > Double.parseDouble(max.toString())) {
                errors.add(ErrorHolderExt.of(EntityErrorCode.ENTITY_ATTRIBUTE_MIN_GREATER_THAN_MAX.getErrorCode(),
                        EntityErrorCode.ENTITY_ATTRIBUTE_MIN_GREATER_THAN_MAX.formatMessage(entityKey, min, max)));
            }
        }
    }
}
