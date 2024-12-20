package com.milesight.beaveriot.rule.manager.model.request;

import lombok.Data;

@Data
public class SaveWorkflowRequest {
    private String id;

    private String name;

    private String remark;

    private String designData;

    private Integer version;
}
