package com.milesight.beaveriot.devicetemplate.po;

import com.milesight.beaveriot.data.support.MapJsonConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Map;


@Data
@Entity
@FieldNameConstants
@Table(name = "t_device_template")
@EntityListeners(AuditingEntityListener.class)
public class DeviceTemplatePO {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(insertable = false, updatable = false)
    private String tenantId;

    private Long userId;

    @Column(name = "\"key\"", length = 512)
    private String key;

    @Column(name = "name")
    private String name;

    @Column(name = "integration")
    private String integration;

    @Column(name = "identifier")
    private String identifier;

    @Column(name = "content")
    private String content;

    @Column(name = "additional_data", length = 1024)
    @Convert(converter = MapJsonConverter.class)
    private Map<String, Object> additionalData;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    @CreatedDate
    private Long createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private Long updatedAt;

    @Column(name = "vendor")
    private String vendor;

    @Column(name = "model")
    private String model;

    @Column(name = "blueprint_library_id")
    private Long blueprintLibraryId;

    @Column(name = "blueprint_library_version")
    private String blueprintLibraryVersion;
}
