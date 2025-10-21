package com.milesight.beaveriot.rule.manager.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;
import com.milesight.beaveriot.rule.manager.po.WorkflowLogPO;

@Tenant
public interface WorkflowLogRepository extends BaseJpaRepository<WorkflowLogPO, Long> {
}
