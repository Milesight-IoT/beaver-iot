package com.milesight.beaveriot.rule.manager.model.request;

import lombok.Data;

import java.util.Map;

@Data
public class TestWorkflowNodeRequest {
    private String nodeConfig;

    private Map<String, Object> input;
}
