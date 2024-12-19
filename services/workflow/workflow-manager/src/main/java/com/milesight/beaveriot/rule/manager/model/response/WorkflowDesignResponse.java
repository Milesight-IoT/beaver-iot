package com.milesight.beaveriot.rule.manager.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowDesignResponse {
    private String id;

    private String name;

    private String remark;

    private Boolean enabled;

    private Integer version;

    private String designData;
}
