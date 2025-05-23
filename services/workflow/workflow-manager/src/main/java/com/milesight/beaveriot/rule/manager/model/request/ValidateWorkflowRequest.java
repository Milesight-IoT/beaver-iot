package com.milesight.beaveriot.rule.manager.model.request;

import com.milesight.beaveriot.rule.manager.support.WorkflowDataFieldConstants;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ValidateWorkflowRequest {
    @Size(max = WorkflowDataFieldConstants.WORKFLOW_DESIGN_MAX_LENGTH)
    private String designData;
}
