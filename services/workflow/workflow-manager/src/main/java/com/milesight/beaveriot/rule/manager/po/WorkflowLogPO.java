package com.milesight.beaveriot.rule.manager.po;

import com.milesight.beaveriot.data.api.SupportTimeSeries;
import com.milesight.beaveriot.data.model.TimeSeriesCategory;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@FieldNameConstants
@Table(name = "t_flow_log")
@EntityListeners(AuditingEntityListener.class)
@SupportTimeSeries(category = TimeSeriesCategory.LOG, timeColumn = WorkflowLogPO.Fields.createdAt, indexedColumns = {
        WorkflowLogPO.Fields.flowId,
        WorkflowLogPO.Fields.id
})
public class WorkflowLogPO {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "flow_id")
    private Long flowId;

    @Column(name = "version")
    private Integer version;

    @Column(name = "start_time")
    private Long startTime;

    @Column(name = "time_cost")
    private Integer timeCost;

    @Column(name = "status", length = 31)
    private String status;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "user_id")
    private Long userId;

    @CreatedDate
    @Column(name = "created_at")
    private Long createdAt;
}
