package com.milesight.beaveriot.rule.manager.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.rule.manager.po.WorkflowEntityRelationPO;

import java.util.List;

public interface WorkflowEntityRelationRepository extends BaseJpaRepository<WorkflowEntityRelationPO, Long> {
    List<WorkflowEntityRelationPO> findAllByEntityIdIn(List<Long> entityIdList);

    List<WorkflowEntityRelationPO> findAllByFlowId(Long flowId);

}
