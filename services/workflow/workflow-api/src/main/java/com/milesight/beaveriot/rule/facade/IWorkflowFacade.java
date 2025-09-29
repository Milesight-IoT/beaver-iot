package com.milesight.beaveriot.rule.facade;

import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.rule.dto.WorkflowNameDTO;

import java.util.List;

/**
 * IWorkflowFacade
 *
 * @author simon
 * @date 2025/9/25
 */
public interface IWorkflowFacade {
    /**
     * Get workflow by <b>parent</b> entity id list.
     *
     * @param entityIdList
     * @return
     */
    List<WorkflowNameDTO> getWorkflowsByEntities(List<Long> entityIdList);

    Entity getTriggerEntityByWorkflow(Long flowId);

}
