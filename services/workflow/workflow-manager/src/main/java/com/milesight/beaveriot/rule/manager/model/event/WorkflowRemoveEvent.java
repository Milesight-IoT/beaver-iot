package com.milesight.beaveriot.rule.manager.model.event;

import lombok.Data;

/**
 * @author leon
 */
@Data
public class WorkflowRemoveEvent extends BaseWorkflowEvent {

    public WorkflowRemoveEvent() {
    }

    public WorkflowRemoveEvent(String tenantId, String id) {
        super(tenantId, id);
    }

}
