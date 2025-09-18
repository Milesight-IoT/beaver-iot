package com.milesight.beaveriot.blueprint.library.po;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * author: Luxb
 * create: 2025/9/16 15:22
 **/
@Data
@Entity
@FieldNameConstants
@Table(name = "t_blueprint_library_address")
@EntityListeners(AuditingEntityListener.class)
public class BlueprintLibraryAddressPO {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "type")
    private String type;

    @Column(name = "url")
    private String url;

    @Column(name = "branch")
    private String branch;

    @Column(name = "active")
    private Boolean active;

    @Column(insertable = false, updatable = false)
    private String tenantId;

    @Column(name = "created_at", updatable = false)
    @CreatedDate
    private Long createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private Long updatedAt;
}