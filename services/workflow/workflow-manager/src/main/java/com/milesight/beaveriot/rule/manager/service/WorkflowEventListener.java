package com.milesight.beaveriot.rule.manager.service;

import com.milesight.beaveriot.rule.manager.po.WorkflowPO;
import com.milesight.beaveriot.rule.manager.repository.WorkflowRepository;
import com.milesight.beaveriot.rule.model.trace.FlowTraceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * WorkflowEventListener class.
 *
 * @author simon
 * @date 2025/8/15
 */
@Service
@Slf4j
public class WorkflowEventListener {
    @Autowired
    WorkflowService workflowService;

    @Autowired
    WorkflowLogService workflowLogService;

    @Autowired
    WorkflowRepository workflowRepository;

    @EventListener
    @Async
    public void onFlowTraceEvent(FlowTraceInfo event) {
        Long flowId = null;
        try {
            flowId = Long.valueOf(event.getFlowId());
        } catch (NumberFormatException e) {
            log.error("Parse flow id error: {}",event.getFlowId());
            return;
        }

        WorkflowPO workflowPO = workflowRepository.findById(flowId).orElse(null);
        if (workflowPO == null) {
            log.error("Cannot find flow {}", flowId);
            return;
        }

        workflowLogService.saveLog(event, workflowPO);

        if (event.isLastExecute()) {
            workflowService.updateStatus(workflowPO, false);
        }
    }
}
