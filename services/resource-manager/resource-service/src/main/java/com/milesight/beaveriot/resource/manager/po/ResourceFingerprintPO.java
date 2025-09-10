package com.milesight.beaveriot.resource.manager.po;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * author: Luxb
 * create: 2025/9/3 16:17
 **/
@Data
@Entity
@FieldNameConstants
@Table(name = "t_resource_fingerprint")
@EntityListeners(AuditingEntityListener.class)
public class ResourceFingerprintPO {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "type")
    private String type;

    @Column(name = "integration")
    private String integration;

    @Column(name = "hash")
    private String hash;

    @Column(name = "created_at", updatable = false)
    @CreatedDate
    private Long createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private Long updatedAt;
}
