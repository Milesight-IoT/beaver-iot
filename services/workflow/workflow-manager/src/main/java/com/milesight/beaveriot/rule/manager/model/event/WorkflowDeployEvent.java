package com.milesight.beaveriot.rule.manager.model.event;

import lombok.Data;

/**
 * @author leon
 */
@Data
public class WorkflowDeployEvent extends BaseWorkflowEvent {

    private String name;

    private String designData;

    public WorkflowDeployEvent() {
    }

    public WorkflowDeployEvent(String tenantId, String id, String name, String designData) {
        super(tenantId, id);
        this.name = name;
        this.designData = designData;
    }

}
