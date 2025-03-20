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

    DASHBOARD_VIEW("dashboard.view", MenuCode.DASHBOARD),
    DASHBOARD_ADD("dashboard.add", MenuCode.DASHBOARD),
    DASHBOARD_EDIT("dashboard.edit", MenuCode.DASHBOARD),

    DEVICE_VIEW("device.view", MenuCode.DEVICE),
    DEVICE_ADD("device.add", MenuCode.DEVICE),
    DEVICE_EDIT("device.edit", MenuCode.DEVICE),
    DEVICE_DELETE("device.delete", MenuCode.DEVICE),

    ENTITY_CUSTOM_VIEW("entity_custom.view", MenuCode.ENTITY_CUSTOM),
    ENTITY_CUSTOM_ADD("entity_custom.add", MenuCode.ENTITY_CUSTOM),
    ENTITY_CUSTOM_EDIT("entity_custom.edit", MenuCode.ENTITY_CUSTOM),
    ENTITY_CUSTOM_DELETE("entity_custom.delete", MenuCode.ENTITY_CUSTOM),

    ENTITY_DATA_VIEW("entity_data.view", MenuCode.ENTITY_DATA),
    ENTITY_DATA_EDIT("entity_data.edit", MenuCode.ENTITY_DATA),
    ENTITY_DATA_EXPORT("entity_data.export", MenuCode.ENTITY_DATA),

    WORKFLOW_VIEW("workflow.view", MenuCode.WORKFLOW),
    WORKFLOW_ADD("workflow.add", MenuCode.WORKFLOW),
    WORKFLOW_EDIT("workflow.edit", MenuCode.WORKFLOW),
    WORKFLOW_DELETE("workflow.delete", MenuCode.WORKFLOW),

    INTEGRATION_VIEW("integration.view", MenuCode.INTEGRATION),
    ;

    private final String code;
    private final MenuCode parent;

}
