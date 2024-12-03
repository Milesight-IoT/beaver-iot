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

    DASHBOARD("dashboard"),
    DASHBOARD_VIEW("dashboard.view"),
    DASHBOARD_ADD("dashboard.add"),
    DASHBOARD_EDIT("dashboard.edit"),
    DEVICE("device"),
    DEVICE_VIEW("device.view"),
    DEVICE_ADD("device.add"),
    DEVICE_RENAME("device.rename"),
    DEVICE_DELETE("device.delete"),
    ENTITY_CUSTOM("entity_custom"),
    ENTITY_CUSTOM_VIEW("entity_custom.view"),
    ENTITY_CUSTOM_ADD("entity_custom.add"),
    ENTITY_CUSTOM_EDIT("entity_custom.edit"),
    ENTITY_CUSTOM_DELETE("entity_custom.delete"),
    ENTITY_DATA("entity_data"),
    ENTITY_DATA_VIEW("entity_data.view"),
    ENTITY_DATA_EDIT("entity_data.edit"),
    ENTITY_DATA_EXPORT("entity_data.export"),
    WORKFLOW("workflow"),
    WORKFLOW_VIEW("workflow.view"),
    WORKFLOW_ADD("workflow.add"),
    WORKFLOW_IMPORT("workflow.import"),
    WORKFLOW_EDIT("workflow.edit"),
    WORKFLOW_EXPORT("workflow.export"),
    WORKFLOW_DELETE("workflow.delete"),
    WORKFLOW_ENABLE("workflow.enable"),
    INTEGRATION("integration"),
    INTEGRATION_VIEW("integration.view"),
    INTEGRATION_EDIT_PROPERTY("integration.edit_property"),
    INTEGRATION_EDIT_SERVICE("integration.edit_service"),
    ;

    private final String code;

}
