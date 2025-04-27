package com.milesight.beaveriot.resource.manager.po;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * ResourceTempPO class.
 *
 * @author simon
 * @date 2025/4/12
 */
@Data
@Entity
@FieldNameConstants
@Table(name = "t_resource_temp")
@EntityListeners(AuditingEntityListener.class)
public class ResourceTempPO {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "settled")
    private Boolean settled;

    @Column(name = "created_at")
    private Long createdAt;

    @Column(name = "expired_at")
    private Long expiredAt;
}
