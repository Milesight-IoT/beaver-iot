package com.milesight.beaveriot.entity.po;

import com.milesight.beaveriot.base.enums.EntityErrorCode;
import com.milesight.beaveriot.base.error.ErrorHolder;
import com.milesight.beaveriot.base.utils.NumberUtils;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.base.utils.ValidationUtils;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.enums.*;
import com.milesight.beaveriot.context.integration.model.AttributeBuilder;
import com.milesight.beaveriot.context.support.EntityValidator;
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
import java.util.HashMap;
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

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR")
    private ValueStoreMod valueStoreMod;

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

    public List<ErrorHolder> validate() {
        List<ErrorHolder> errors = new ArrayList<>();
        String entityKey = getKey();
        if (entityKey == null) {
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_KEY_NULL.getErrorCode(),
                    EntityErrorCode.ENTITY_KEY_NULL.getErrorMessage()));
            return errors;
        }
        Map<String, Object> entityData = Map.of(ExtraDataConstants.KEY_ENTITY_KEY, entityKey);

        try {
            if (type == null) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_TYPE_NULL.getErrorCode(),
                        EntityErrorCode.ENTITY_TYPE_NULL.formatMessage(entityKey),
                        entityData));
            } else {
                if (type.equals(EntityType.PROPERTY) && accessMod == null) {
                    errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ACCESS_MOD_NULL.getErrorCode(),
                            EntityErrorCode.ENTITY_ACCESS_MOD_NULL.formatMessage(entityKey),
                            entityData));
                }
            }

            if (valueStoreMod == null) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_STORE_MOD_NULL.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_STORE_MOD_NULL.formatMessage(entityKey),
                        entityData));
            }

            if (valueType == null) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_TYPE_NULL.getErrorCode(),
                        EntityErrorCode.ENTITY_VALUE_TYPE_NULL.formatMessage(entityKey),
                        entityData));
            }

            if (StringUtils.isEmpty(name)) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_NAME_EMPTY.getErrorCode(),
                        EntityErrorCode.ENTITY_NAME_EMPTY.formatMessage(entityKey),
                        entityData));
            }

            if (attachTarget == null) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTACH_TARGET_NULL.getErrorCode(),
                        EntityErrorCode.ENTITY_ATTACH_TARGET_NULL.formatMessage(entityKey),
                        entityData));
            }

            if (attachTargetId == null) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTACH_TARGET_ID_NULL.getErrorCode(),
                        EntityErrorCode.ENTITY_ATTACH_TARGET_ID_NULL.formatMessage(entityKey),
                        entityData));
            }

            if (valueAttribute != null) {
                validateAttributeMinAndMax(entityKey, entityData, errors);
                validateAttributeMinLengthAndMaxLength(entityKey, entityData, errors);
                validateAttributeLengthRange(entityKey, entityData, errors);
                validateAttributeFractionDigits(entityKey, entityData, errors);
                validateAttributeDefaultValue(entityKey, entityData, errors);
                validateAttributeOptional(entityKey, entityData, errors);
                validateAttributeEnum(entityKey, entityData, errors);
                validateAttributeImportant(entityKey, entityData, errors);
            }

            if (checkIsCustomizedEntity()) {
                if (valueAttribute != null) {
                    validateAttributeKeys(entityKey, entityData, errors);
                    validateAttributeIsEnum(entityKey, entityData, errors);
                    validateAttributeUnit(entityKey, entityData, errors);
                }
            }
        } catch (Exception e) {
            errors.clear();
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_VALUE_VALIDATION_ERROR.getErrorCode(),
                    EntityErrorCode.ENTITY_VALIDATION_ERROR.formatMessage(entityKey, e.getMessage()),
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
        public static final String KEY_VALUE_TYPE = "value_type";
        public static final String KEY_ALLOWED_ATTRIBUTES = "allowed_attributes";
        public static final String KEY_ATTRIBUTE_ENUM_MAX_SIZE = "attribute_enum_max_size";
        public static final String KEY_ATTRIBUTE_ENUM_STRING_MAX_LENGTH = "attribute_enum_string_max_length";
        public static final String KEY_ATTRIBUTE_UNIT_STRING_MAX_LENGTH = "attribute_unit_string_max_length";
    }

    private void validateAttributeUnit(String entityKey, Map<String, Object> entityData, List<ErrorHolder> errors) {
        Object unit = valueAttribute.get(AttributeBuilder.ATTRIBUTE_UNIT);
        if (unit == null) {
            return;
        }

        if (unit.toString().length() > EntityDataFieldConstants.CUSTOM_ENTITY_UNIT_STRING_MAX_LENGTH) {
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_UNIT_TOO_LONG.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_UNIT_TOO_LONG.formatMessage(entityKey, EntityDataFieldConstants.CUSTOM_ENTITY_UNIT_STRING_MAX_LENGTH),
                    buildExtraData(entityData, Map.of(
                            ExtraDataConstants.KEY_ATTRIBUTE_UNIT_STRING_MAX_LENGTH, EntityDataFieldConstants.CUSTOM_ENTITY_UNIT_STRING_MAX_LENGTH
                    ))));
        }
    }

    private void validateAttributeIsEnum(String entityKey, Map<String, Object> entityData, List<ErrorHolder> errors) {
        Object isEnum = valueAttribute.get(EntityDataFieldConstants.CUSTOM_ENTITY_ATTRIBUTE_IS_ENUM);
        if (isEnum == null) {
            return;
        }

        if (!(isEnum instanceof Boolean)) {
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_IS_ENUM_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_IS_ENUM_INVALID.formatMessage(entityKey),
                    entityData));
        }
    }

    private void validateAttributeKeys(String entityKey, Map<String, Object> entityData, List<ErrorHolder> errors) {
        if (!EntityDataFieldConstants.CUSTOM_ENTITY_ALLOWED_ATTRIBUTES.containsAll(valueAttribute.keySet())) {
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_KEY_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_KEY_INVALID.formatMessage(entityKey, "{" + String.join(", ", EntityDataFieldConstants.CUSTOM_ENTITY_ALLOWED_ATTRIBUTES) + "}"),
                    buildExtraData(entityData, Map.of(
                            ExtraDataConstants.KEY_ALLOWED_ATTRIBUTES, EntityDataFieldConstants.CUSTOM_ENTITY_ALLOWED_ATTRIBUTES
                    ))));
        }
    }

    @SuppressWarnings("unchecked")
    private void validateAttributeEnum(String entityKey, Map<String, Object> entityData, List<ErrorHolder> errors) {
        Object enums = valueAttribute.get(AttributeBuilder.ATTRIBUTE_ENUM);
        if (enums == null) {
            return;
        }

        if (!(enums instanceof Map)) {
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_ENUM_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_ENUM_INVALID.formatMessage(entityKey),
                    entityData));
        } else {
            if (checkIsCustomizedEntity()) {
                Map<String, String> enumsMap = (Map<String, String>) enums;
                if (enumsMap.size() > EntityDataFieldConstants.CUSTOM_ENTITY_ENUM_MAX_SIZE) {
                    errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_ENUM_OVER_SIZE.getErrorCode(),
                            EntityErrorCode.ENTITY_ATTRIBUTE_ENUM_OVER_SIZE.formatMessage(entityKey, EntityDataFieldConstants.CUSTOM_ENTITY_ENUM_MAX_SIZE),
                            buildExtraData(entityData, Map.of(
                                    ExtraDataConstants.KEY_ATTRIBUTE_ENUM_MAX_SIZE, EntityDataFieldConstants.CUSTOM_ENTITY_ENUM_MAX_SIZE
                            ))));
                }

                for (Map.Entry<String, String> entry : enumsMap.entrySet()) {
                    if (entry.getKey().length() > EntityDataFieldConstants.CUSTOM_ENTITY_ENUM_STRING_MAX_LENGTH ||
                                    entry.getValue().length() > EntityDataFieldConstants.CUSTOM_ENTITY_ENUM_STRING_MAX_LENGTH) {
                        errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_ENUM_KEY_OR_VALUE_LENGTH_GREATER_THAN_MAX_LENGTH.getErrorCode(),
                                EntityErrorCode.ENTITY_ATTRIBUTE_ENUM_KEY_OR_VALUE_LENGTH_GREATER_THAN_MAX_LENGTH.formatMessage(entityKey, EntityDataFieldConstants.CUSTOM_ENTITY_ENUM_STRING_MAX_LENGTH),
                                buildExtraData(entityData, Map.of(
                                        ExtraDataConstants.KEY_ATTRIBUTE_ENUM_STRING_MAX_LENGTH, EntityDataFieldConstants.CUSTOM_ENTITY_ENUM_STRING_MAX_LENGTH
                                ))
                        ));
                        break;
                    }
                }
            }
        }
    }

    private void validateAttributeOptional(String entityKey, Map<String, Object> entityData, List<ErrorHolder> errors) {
        Object optional = valueAttribute.get(AttributeBuilder.ATTRIBUTE_OPTIONAL);
        if (optional == null) {
            return;
        }

        if (!(optional instanceof Boolean)) {
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_OPTIONAL_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_OPTIONAL_INVALID.formatMessage(entityKey),
                    entityData));
        }
    }

    private void validateAttributeDefaultValue(String entityKey, Map<String, Object> entityData, List<ErrorHolder> errors) {
        Object defaultValue = valueAttribute.get(AttributeBuilder.ATTRIBUTE_DEFAULT_VALUE);
        if (defaultValue == null) {
            return;
        }

        if (!EntityValidator.isMatchType(valueType, defaultValue)) {
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_DEFAULT_VALUE_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_DEFAULT_VALUE_INVALID.formatMessage(entityKey, valueType.name()),
                    buildExtraData(entityData, Map.of(
                            ExtraDataConstants.KEY_VALUE_TYPE, valueType.name()
                    ))));
        }
    }

    private void validateAttributeFractionDigits(String entityKey, Map<String, Object> entityData, List<ErrorHolder> errors) {
        Object fractionDigits = valueAttribute.get(AttributeBuilder.ATTRIBUTE_FRACTION_DIGITS);
        if (fractionDigits == null) {
            return;
        }

        if (!ValidationUtils.isNonNegativeInteger(fractionDigits.toString())) {
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_FRACTION_DIGITS_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_FRACTION_DIGITS_INVALID.formatMessage(entityKey),
                    entityData));
        }
    }

    private void validateAttributeLengthRange(String entityKey, Map<String, Object> entityData, List<ErrorHolder> errors) {
        Object lengthRange = valueAttribute.get(AttributeBuilder.ATTRIBUTE_LENGTH_RANGE);
        if (lengthRange == null) {
            return;
        }

        if (!(lengthRange instanceof String)) {
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_LENGTH_RANGE_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_LENGTH_RANGE_INVALID.formatMessage(entityKey),
                    entityData));
            return;
        }

        String[] lengthRangeArray = lengthRange.toString().split(",");
        for (String lengthRangeItem : lengthRangeArray) {
            if (!ValidationUtils.isPositiveInteger(lengthRangeItem)) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_LENGTH_RANGE_INVALID.getErrorCode(),
                        EntityErrorCode.ENTITY_ATTRIBUTE_LENGTH_RANGE_INVALID.formatMessage(entityKey),
                        entityData));
                return;
            }
        }
    }

    private void validateAttributeMinLengthAndMaxLength(String entityKey, Map<String, Object> entityData, List<ErrorHolder> errors) {
        boolean isMinLengthValid = true;
        Object minLength = valueAttribute.get(AttributeBuilder.ATTRIBUTE_MIN_LENGTH);
        if (minLength != null && !ValidationUtils.isPositiveInteger(minLength.toString())) {
            isMinLengthValid = false;
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_MIN_LENGTH_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_MIN_LENGTH_INVALID.formatMessage(entityKey),
                    entityData));
        }

        boolean isMaxLengthValid = true;
        Object maxLength = valueAttribute.get(AttributeBuilder.ATTRIBUTE_MAX_LENGTH);
        if (maxLength != null && !ValidationUtils.isPositiveInteger(maxLength.toString())) {
            isMaxLengthValid = false;
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_MAX_LENGTH_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_MAX_LENGTH_INVALID.formatMessage(entityKey),
                    entityData));
        }

        if (minLength != null && maxLength != null && isMinLengthValid && isMaxLengthValid) {
            if (Integer.parseInt(minLength.toString()) > Integer.parseInt(maxLength.toString())) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_MIN_LENGTH_GREATER_THAN_MAX_LENGTH.getErrorCode(),
                        EntityErrorCode.ENTITY_ATTRIBUTE_MIN_LENGTH_GREATER_THAN_MAX_LENGTH.formatMessage(entityKey, minLength, maxLength),
                        buildExtraData(entityData, Map.of(
                                AttributeBuilder.ATTRIBUTE_MIN_LENGTH, minLength,
                                AttributeBuilder.ATTRIBUTE_MAX_LENGTH, maxLength
                        ))));
            }
        }
    }

    private void validateAttributeMinAndMax(String entityKey, Map<String, Object> entityData, List<ErrorHolder> errors) {
        boolean isMinValid = true;
        Object min = valueAttribute.get(AttributeBuilder.ATTRIBUTE_MIN);
        Double minValue = NumberUtils.parseDouble(min);
        if (min != null && minValue == null) {
            isMinValid = false;
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_MIN_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_MIN_INVALID.formatMessage(entityKey),
                    entityData));
        }

        boolean isMaxValid = true;
        Object max = valueAttribute.get(AttributeBuilder.ATTRIBUTE_MAX);
        Double maxValue = NumberUtils.parseDouble(max);
        if (max != null && maxValue == null) {
            isMaxValid = false;
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_MAX_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_MAX_INVALID.formatMessage(entityKey),
                    entityData));
        }

        if (min != null && max != null && isMinValid && isMaxValid) {
            if (minValue > maxValue) {
                errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_MIN_GREATER_THAN_MAX.getErrorCode(),
                        EntityErrorCode.ENTITY_ATTRIBUTE_MIN_GREATER_THAN_MAX.formatMessage(entityKey, min, max),
                        buildExtraData(entityData, Map.of(
                                AttributeBuilder.ATTRIBUTE_MIN, min, AttributeBuilder.ATTRIBUTE_MAX, max
                        ))));
            }
        }
    }

    private void validateAttributeImportant(String entityKey, Map<String, Object> entityData, List<ErrorHolder> errors) {
        Object important = valueAttribute.get(AttributeBuilder.ATTRIBUTE_IMPORTANT);
        if (important == null) {
            return;
        }

        if (!ValidationUtils.isPositiveInteger(important.toString())) {
            errors.add(ErrorHolder.of(EntityErrorCode.ENTITY_ATTRIBUTE_IMPORTANT_INVALID.getErrorCode(),
                    EntityErrorCode.ENTITY_ATTRIBUTE_IMPORTANT_INVALID.formatMessage(entityKey),
                    entityData));
        }
    }
}
