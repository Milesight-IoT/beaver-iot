package com.milesight.beaveriot.entity.po;

import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.data.support.MapJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
public class EntityPO {

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

    @CreatedDate
    private Long createdAt;

    @LastModifiedDate
    private Long updatedAt;

}
