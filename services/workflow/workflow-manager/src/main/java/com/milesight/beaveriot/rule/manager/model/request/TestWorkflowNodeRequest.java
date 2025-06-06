package com.milesight.beaveriot.rule.manager.model.request;

import com.milesight.beaveriot.rule.manager.support.WorkflowDataFieldConstants;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class TestWorkflowNodeRequest {
    @Size(max = WorkflowDataFieldConstants.WORKFLOW_DESIGN_MAX_LENGTH)
    private String nodeConfig;

    private Map<String, Object> input;
}
