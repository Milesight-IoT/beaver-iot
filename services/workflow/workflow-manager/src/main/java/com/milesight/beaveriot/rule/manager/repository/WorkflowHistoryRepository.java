package com.milesight.beaveriot.rule.manager.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.rule.manager.po.WorkflowHistoryPO;

import java.util.List;

public interface WorkflowHistoryRepository extends BaseJpaRepository<WorkflowHistoryPO, Long> {
    void deleteByFlowIdIn(List<Long> flowId);
}
