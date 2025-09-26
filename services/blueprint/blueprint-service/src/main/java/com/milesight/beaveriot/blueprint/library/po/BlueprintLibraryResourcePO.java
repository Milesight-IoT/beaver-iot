package com.milesight.beaveriot.blueprint.library.po;

import jakarta.persistence.*;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * author: Luxb
 * create: 2025/9/1 9:37
 **/
@Data
@Entity
@FieldNameConstants
@Table(name = "t_blueprint_library_resource")
@EntityListeners(AuditingEntityListener.class)
public class BlueprintLibraryResourcePO {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "path")
    private String path;

    @Column(name = "content")
    private String content;

    @Column(name = "library_id")
    private Long libraryId;

    @Column(name = "library_version")
    private String libraryVersion;

    @Column(name = "created_at")
    @CreatedDate
    private Long createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private Long updatedAt;
}