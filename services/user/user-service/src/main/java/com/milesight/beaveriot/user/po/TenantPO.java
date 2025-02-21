package com.milesight.beaveriot.user.po;

import com.milesight.beaveriot.user.enums.TenantStatus;
import jakarta.persistence.Column;
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

/**
 * @author loong
 * @date 2024/11/19 16:06
 */
@Data
@Table(name = "t_tenant")
@Entity
@FieldNameConstants
@EntityListeners(AuditingEntityListener.class)
public class TenantPO {

    @Id
    private String id;
    private String name;
    private String domain;
    private String timeZone;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR", length = 32)
    private TenantStatus status;
    @CreatedDate
    private Long createdAt;
    @LastModifiedDate
    private Long updatedAt;
}
