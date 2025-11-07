package com.milesight.beaveriot.entitytemplate.po;

import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.enums.ValueStoreMod;
import com.milesight.beaveriot.data.support.MapJsonConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Map;
import java.util.Objects;

/**
 * author: Luxb
 * create: 2025/8/20 9:36
 **/
@Data
@Entity
@FieldNameConstants
@Table(name = "t_entity_template")
@EntityListeners(AuditingEntityListener.class)
public class EntityTemplatePO {
    @Id
    private Long id;

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
    private EntityValueType valueType;

    @Convert(converter = MapJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> valueAttribute;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Boolean visible = true;

    @Column(insertable = false, updatable = false)
    private String tenantId;

    private Long userId;

    @CreatedDate
    private Long createdAt;

    @LastModifiedDate
    private Long updatedAt;

    public boolean logicEquals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntityTemplatePO that)) {
            return false;
        }

        return Objects.equals(name, that.getName()) &&
                type == that.getType() &&
                accessMod == that.getAccessMod() &&
                Objects.equals(valueStoreMod, that.getValueStoreMod()) &&
                Objects.equals(parent, that.getParent()) &&
                valueType == that.getValueType() &&
                Objects.equals(valueAttribute, that.getValueAttribute()) &&
                Objects.equals(description, that.getDescription()) &&
                Objects.equals(visible, that.getVisible());
    }
}
