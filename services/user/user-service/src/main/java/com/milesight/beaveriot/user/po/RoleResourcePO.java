package com.milesight.beaveriot.user.po;

import com.milesight.beaveriot.user.enums.ResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @author loong
 * @date 2024/11/19 15:43
 */
@Data
@Table(name = "t_role_resource")
@Entity
@FieldNameConstants
@EntityListeners(AuditingEntityListener.class)
public class RoleResourcePO {

    @Id
    private Long id;
    @Column(insertable = false, updatable = false)
    private String tenantId;
    private Long roleId;
    @Column(length = 255)
    private String resourceId;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR", length = 32)
    private ResourceType resourceType;
    @CreatedDate
    private Long createdAt;

}
