package com.milesight.beaveriot.rule.manager.po;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Mainly used for restoring the design at the time of log generation
 */
@Data
@Entity
@FieldNameConstants
@Table(name = "t_flow_history")
@EntityListeners(AuditingEntityListener.class)
public class WorkflowHistoryPO {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "flow_id")
    private Long flowId;

    @Column(name = "version")
    private Integer version;

    @Column(name = "design_data", columnDefinition = "TEXT")
    private String designData;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at")
    @CreatedDate
    private Long createdAt;
}
