package com.milesight.beaveriot.rule.manager.service;

import com.milesight.beaveriot.rule.dto.WorkflowNameDTO;
import com.milesight.beaveriot.rule.facade.IWorkflowFacade;
import com.milesight.beaveriot.rule.manager.po.WorkflowEntityRelationPO;
import com.milesight.beaveriot.rule.manager.po.WorkflowPO;
import com.milesight.beaveriot.rule.manager.repository.WorkflowEntityRelationRepository;
import com.milesight.beaveriot.rule.manager.repository.WorkflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * WorkflowFacadeImpl class.
 *
 * @author simon
 * @date 2025/9/25
 */
@Component
public class WorkflowFacadeImpl implements IWorkflowFacade {
    @Autowired
    WorkflowEntityRelationRepository workflowEntityRelationRepository;

    @Autowired
    WorkflowRepository workflowRepository;

    @Override
    public List<WorkflowNameDTO> getWorkflowsByEntities(List<Long> entityIdList) {
        List<WorkflowEntityRelationPO> relationPOList = workflowEntityRelationRepository.findAllByEntityIdIn(entityIdList);
        Map<Long, WorkflowPO> workflowPOMap = workflowRepository
                .findByIdIn(relationPOList.stream().map(WorkflowEntityRelationPO::getFlowId).toList())
                .stream().collect(Collectors.toMap(WorkflowPO::getId, Function.identity()));
        List<WorkflowNameDTO> result = new ArrayList<>();
        relationPOList.forEach(relationPO -> {
            WorkflowPO workflowPO = workflowPOMap.get(relationPO.getFlowId());
            if (workflowPO != null) {
                result.add(WorkflowNameDTO
                        .builder()
                                .workflowId(relationPO.getFlowId())
                                .entityId(relationPO.getEntityId())
                                .name(workflowPO.getName())
                        .build());
            }
        });

        return result;
    }
}
