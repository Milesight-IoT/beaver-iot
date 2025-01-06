package com.milesight.beaveriot.rule.manager.po;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@FieldNameConstants
@Table(name = "t_flow_log_data")
@EntityListeners(AuditingEntityListener.class)
public class WorkflowLogDataPO {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "data", columnDefinition = "TEXT")
    private String data;

    @CreatedDate
    @Column(name = "created_at")
    private Long createdAt;
}
