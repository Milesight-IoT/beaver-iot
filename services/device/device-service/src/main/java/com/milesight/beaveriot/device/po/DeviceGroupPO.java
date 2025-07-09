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
@Table(name = "t_device_group")
@EntityListeners(AuditingEntityListener.class)
public class DeviceGroupPO {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name", length = 1024, nullable = false)
    private String name;

    @Column(insertable = false, updatable = false)
    private String tenantId;

    @Column(name = "created_at")
    @CreatedDate
    private Long createdAt;
}
