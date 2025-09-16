package com.milesight.beaveriot.blueprint.library.po;

import com.milesight.beaveriot.blueprint.model.BlueprintLibrarySyncStatus;
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
 * create: 2025/9/1 9:29
 **/
@Data
@Entity
@FieldNameConstants
@Table(name = "t_blueprint_library")
@EntityListeners(AuditingEntityListener.class)
public class BlueprintLibraryPO {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "home")
    private String home;

    @Column(name = "branch")
    private String branch;

    @Column(name = "current_version")
    private String currentVersion;

    @Column(name = "remote_version")
    private String remoteVersion;

    @Column(name = "sync_status")
    @Enumerated(EnumType.STRING)
    private BlueprintLibrarySyncStatus syncStatus;

    @Column(name = "synced_at")
    private Long syncedAt;

    @Column(name = "created_at", updatable = false)
    @CreatedDate
    private Long createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private Long updatedAt;
}