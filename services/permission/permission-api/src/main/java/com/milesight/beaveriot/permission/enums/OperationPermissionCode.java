package com.milesight.beaveriot.permission.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author loong
 * @date 2024/12/3 16:44
 */
@AllArgsConstructor
@Getter
public enum OperationPermissionCode {

    DASHBOARD_VIEW("dashboard.view"),
    DASHBOARD_ADD("dashboard.add"),
    DASHBOARD_EDIT("dashboard.edit"),

    DEVICE_VIEW("device.view"),
    DEVICE_ADD("device.add"),
    DEVICE_EDIT("device.edit"),
    DEVICE_DELETE("device.delete"),

    ENTITY_CUSTOM_VIEW("entity_custom.view"),
    ENTITY_CUSTOM_ADD("entity_custom.add"),
    ENTITY_CUSTOM_EDIT("entity_custom.edit"),
    ENTITY_CUSTOM_DELETE("entity_custom.delete"),

    ENTITY_DATA_VIEW("entity_data.view"),
    ENTITY_DATA_EDIT("entity_data.edit"),
    ENTITY_DATA_EXPORT("entity_data.export"),

    WORKFLOW_VIEW("workflow.view"),
    WORKFLOW_ADD("workflow.add"),
    WORKFLOW_EDIT("workflow.edit"),
    WORKFLOW_DELETE("workflow.delete"),

    INTEGRATION_VIEW("integration.view"),
    ;

    private final String code;

}
