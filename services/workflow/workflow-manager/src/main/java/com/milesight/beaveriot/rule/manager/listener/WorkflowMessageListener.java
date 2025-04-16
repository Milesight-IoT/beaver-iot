package com.milesight.beaveriot.rule.manager.listener;

import com.milesight.beaveriot.pubsub.api.annotation.MessageListener;
import com.milesight.beaveriot.rule.manager.model.event.WorkflowDeployEvent;
import com.milesight.beaveriot.rule.manager.model.event.WorkflowRemoveEvent;
import com.milesight.beaveriot.rule.manager.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author leon
 */
@Slf4j
@Component
public class WorkflowMessageListener {

    @Autowired
    private WorkflowService workflowService;

    @MessageListener
    public void onWorkflowDeployEvent(WorkflowDeployEvent event) {
        if (event.isSelfInstance()) {
            return;
        }
        log.debug("Received workflow deploy event: " + event);
        workflowService.deployFlow(event);
    }

    @MessageListener
    public void onWorkflowRemoveEvent(WorkflowRemoveEvent event) {
        if (event.isSelfInstance()) {
            return;
        }
        log.debug("Received workflow remove event: " + event);
        workflowService.removeFlow(event);
    }

}
