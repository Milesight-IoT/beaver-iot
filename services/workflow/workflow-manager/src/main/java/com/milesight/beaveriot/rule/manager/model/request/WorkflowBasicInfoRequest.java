package com.milesight.beaveriot.rule.manager.model.request;

import com.milesight.beaveriot.rule.manager.support.WorkflowDataFieldConstants;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkflowBasicInfoRequest {
    @Size(max = WorkflowDataFieldConstants.WORKFLOW_NAME_MAX_LENGTH)
    private String name;

    @Size(max = WorkflowDataFieldConstants.WORKFLOW_REMARK_MAX_LENGTH)
    private String remark;
}
