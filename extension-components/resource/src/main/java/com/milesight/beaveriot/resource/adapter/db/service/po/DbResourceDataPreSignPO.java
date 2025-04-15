package com.milesight.beaveriot.resource.adapter.db.service.po;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * DbResourceDataPreSignPO class.
 *
 * @author simon
 * @date 2025/4/12
 */
@Data
@Entity
@FieldNameConstants
@Table(name = "t_resource_data_pre_sign")
@EntityListeners(AuditingEntityListener.class)
public class DbResourceDataPreSignPO {
    @Id
    @Column(name = "obj_key", length = 512)
    private String objKey;

    @Column(name = "expired_at")
    private Long expiredAt;

    @Column(name = "created_at")
    @CreatedDate
    private Long createdAt;
}
