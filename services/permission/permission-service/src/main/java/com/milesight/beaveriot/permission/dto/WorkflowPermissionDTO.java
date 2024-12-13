package com.milesight.beaveriot.permission.dto;

import lombok.Data;

import java.util.List;

/**
 * @author loong
 * @date 2024/12/13 10:20
 */
@Data
public class WorkflowPermissionDTO {

    private boolean isHasAllPermission;
    private List<String> workflowIds;

}
