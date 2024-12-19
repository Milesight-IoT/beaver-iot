package com.milesight.beaveriot.rule.manager.po;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@FieldNameConstants
@Table(name = "t_flow_entity_relation")
@EntityListeners(AuditingEntityListener.class)
public class WorkflowEntityRelationPO {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "flow_id")
    private Long flowId;

    @CreatedDate
    @Column(name = "created_at")
    private Long createdAt;
}
