package com.milesight.beaveriot.entity.po;

import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.AttributeBuilder;
import com.milesight.beaveriot.data.support.MapJsonConverter;
import com.milesight.beaveriot.entity.constants.EntityDataFieldConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
}
