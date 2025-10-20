package com.milesight.beaveriot.rule.manager.repository;

import com.milesight.beaveriot.data.api.SupportTimeSeries;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.data.model.TimeSeriesCategory;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.rule.manager.po.WorkflowLogPO;

@Tenant
@SupportTimeSeries(category = TimeSeriesCategory.LOG, entity = WorkflowLogPO.class, timeColumn = WorkflowLogPO.Fields.createdAt, indexedColumns = {
        WorkflowLogPO.Fields.flowId,
        WorkflowLogPO.Fields.id
})
public interface WorkflowLogRepository extends BaseJpaRepository<WorkflowLogPO, Long> {
}
