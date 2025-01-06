package com.milesight.beaveriot.rule.manager.model.response;

import lombok.Data;

@Data
public class SaveWorkflowResponse {
    private String flowId;

    private Integer version;
}
