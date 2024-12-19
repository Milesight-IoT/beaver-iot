package com.milesight.beaveriot.rule.manager.model.request;

import lombok.Data;

import java.util.Map;

@Data
public class TestWorkflowRequest {
    private Map<String, Object> input;

    private String designData;
}
