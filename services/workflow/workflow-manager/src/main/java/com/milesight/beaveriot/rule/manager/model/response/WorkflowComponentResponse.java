package com.milesight.beaveriot.rule.manager.model.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WorkflowComponentResponse {
    private Map<String, List<WorkflowComponent>> entry;
}
