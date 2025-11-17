package com.milesight.beaveriot.rule.manager.po;

import com.milesight.beaveriot.data.api.SupportTimeSeries;
import com.milesight.beaveriot.data.model.TimeSeriesCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@FieldNameConstants
@Table(name = "t_flow_log_data")
@EntityListeners(AuditingEntityListener.class)
@SupportTimeSeries(category = TimeSeriesCategory.LOG, timeColumn = WorkflowLogDataPO.Fields.createdAt, indexedColumns = {
        WorkflowLogDataPO.Fields.id
})
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
