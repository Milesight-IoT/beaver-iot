package com.milesight.beaveriot.device.po;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * author: Luxb
 * create: 2025/9/9 14:52
 **/
@Data
@Entity
@FieldNameConstants
@Table(name = "t_device_blueprint_mapping")
@EntityListeners(AuditingEntityListener.class)
public class DeviceBlueprintMappingPO {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "blueprint_id", nullable = false)
    private Long blueprintId;

    @Column(insertable = false, updatable = false)
    private String tenantId;

    @Column(name = "created_at", updatable = false)
    @CreatedDate
    private Long createdAt;
}
