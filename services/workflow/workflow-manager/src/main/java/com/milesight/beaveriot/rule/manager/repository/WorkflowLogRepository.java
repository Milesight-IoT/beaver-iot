package com.milesight.beaveriot.rule.manager.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.rule.manager.po.WorkflowLogPO;

import java.util.List;

public interface WorkflowLogRepository extends BaseJpaRepository<WorkflowLogPO, Long> {
}
