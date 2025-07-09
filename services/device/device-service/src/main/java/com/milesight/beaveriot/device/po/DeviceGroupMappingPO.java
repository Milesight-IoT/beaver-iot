package com.milesight.beaveriot.device.po;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * DeviceGroupPO class.
 *
 * @author simon
 * @date 2025/6/25
 */
@Data
@Entity
@FieldNameConstants
@Table(name = "t_device_group_mapping")
@EntityListeners(AuditingEntityListener.class)
public class DeviceGroupMappingPO {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(insertable = false, updatable = false)
    private String tenantId;

    @Column(name = "created_at")
    @CreatedDate
    private Long createdAt;
}
