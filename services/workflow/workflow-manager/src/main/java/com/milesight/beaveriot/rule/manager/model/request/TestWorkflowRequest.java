package com.milesight.beaveriot.rule.manager.model.request;

import com.milesight.beaveriot.rule.manager.support.WorkflowDataFieldConstants;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class TestWorkflowRequest {
    @Size(max = WorkflowDataFieldConstants.WORKFLOW_DESIGN_MAX_LENGTH)
    private Map<String, Object> input;

    private String designData;
}
