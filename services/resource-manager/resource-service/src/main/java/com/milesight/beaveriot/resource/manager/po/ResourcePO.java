package com.milesight.beaveriot.resource.manager.po;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * ResourcePO class.
 *
 * @author simon
 * @date 2025/4/1
 */
@Data
@Entity
@FieldNameConstants
@Table(name = "t_resource")
@EntityListeners(AuditingEntityListener.class)
public class ResourcePO {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "\"key\"", length = 512)
    private String key;

    @Column(name = "url", length = 512)
    private String url;

    @Column(insertable = false, updatable = false)
    private String tenantId;

    @Column(name = "name", length = 512)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "content_length")
    private Long contentLength;

    @Column(name = "created_at")
    @CreatedDate
    private Long createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    @LastModifiedDate
    private Long updatedAt;

    private String updatedBy;
}
