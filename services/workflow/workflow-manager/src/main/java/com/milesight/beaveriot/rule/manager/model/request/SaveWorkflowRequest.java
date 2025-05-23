package com.milesight.beaveriot.rule.manager.model.request;

import com.milesight.beaveriot.rule.manager.support.WorkflowDataFieldConstants;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveWorkflowRequest {
    private String id;

    @Size(max = WorkflowDataFieldConstants.WORKFLOW_NAME_MAX_LENGTH)
    private String name;

    @Size(max = WorkflowDataFieldConstants.WORKFLOW_REMARK_MAX_LENGTH)
    private String remark;

    private Boolean enabled = false;

    @Size(max = WorkflowDataFieldConstants.WORKFLOW_DESIGN_MAX_LENGTH)
    private String designData;

    private Integer version;
}
