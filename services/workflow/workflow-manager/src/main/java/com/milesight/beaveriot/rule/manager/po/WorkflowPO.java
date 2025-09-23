package com.milesight.beaveriot.rule.manager.po;

import com.milesight.beaveriot.rule.manager.converter.WorkflowAdditionalDataConverter;
import com.milesight.beaveriot.rule.manager.model.WorkflowAdditionalData;
import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@FieldNameConstants
@Table(name = "t_flow")
@EntityListeners(AuditingEntityListener.class)
public class WorkflowPO {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "version")
    private Integer version;

    @Column(name = "name")
    private String name;

    @Column(name = "remark")
    private String remark;

    @Column(name = "design_data", columnDefinition = "TEXT")
    private String designData;

    @Column(name = "additional_data")
    @Convert(converter = WorkflowAdditionalDataConverter.class)
    private WorkflowAdditionalData additionalData;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "tenant_id", insertable = false, updatable = false)
    private String tenantId;

    @Column(name = "updated_user")
    private Long updatedUser;

    @Column(name = "user_id")
    private Long userId;

    @CreatedDate
    @Column(name = "created_at")
    private Long createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Long updatedAt;
}
