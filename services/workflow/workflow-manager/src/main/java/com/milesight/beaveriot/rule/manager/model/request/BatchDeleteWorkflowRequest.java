package com.milesight.beaveriot.rule.manager.model.request;

import lombok.Data;

import java.util.List;

@Data
public class BatchDeleteWorkflowRequest {
    private List<String> workflowIdList;
}
