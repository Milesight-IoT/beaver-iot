package com.milesight.beaveriot.rule.manager.model.response;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class WorkflowLogResponse {
    private String id;

    private Long startTime;

    private Integer timeCost;

    private String status;

    private Integer version;

    private String message;
}
