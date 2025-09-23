package com.milesight.beaveriot.rule.manager.model.response;

import com.milesight.beaveriot.rule.manager.model.WorkflowAdditionalData;
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

    private WorkflowAdditionalData additionalData;
}
