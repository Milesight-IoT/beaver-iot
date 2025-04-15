package com.milesight.beaveriot.resource.manager.po;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * ResourceRefPO class.
 *
 * @author simon
 * @date 2025/4/12
 */
@Data
@Entity
@FieldNameConstants
@Table(name = "t_resource_ref")
@EntityListeners(AuditingEntityListener.class)
public class ResourceRefPO {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "ref_id")
    private String refId;

    @Column(name = "ref_type")
    private String refType;

    @Column(name = "created_at")
    @CreatedDate
    private Long createdAt;
}
