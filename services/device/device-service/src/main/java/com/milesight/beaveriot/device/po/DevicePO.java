package com.milesight.beaveriot.device.po;

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
@Table(name = "t_device")
@EntityListeners(AuditingEntityListener.class)
public class DevicePO {

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

    @Column(name = "template")
    private String template;

    @Column(name = "additional_data", length = 1024)
    @Convert(converter = MapJsonConverter.class)
    private Map<String, Object> additionalData;

    @Column(name = "created_at")
    @CreatedDate
    private Long createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private Long updatedAt;
}
