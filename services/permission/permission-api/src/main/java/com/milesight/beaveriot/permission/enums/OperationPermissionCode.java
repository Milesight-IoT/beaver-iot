package com.milesight.beaveriot.permission.enums;

import lombok.*;

/**
 * @author loong
 * @date 2024/12/3 16:44
 */
@AllArgsConstructor
@Getter
public enum OperationPermissionCode implements MenuItem {

    DASHBOARD_VIEW(1001L, "dashboard.view", MenuCode.DASHBOARD),
    DASHBOARD_ADD(1002L, "dashboard.add", MenuCode.DASHBOARD),
    DASHBOARD_EDIT(1003L, "dashboard.edit", MenuCode.DASHBOARD),
    DASHBOARD_DELETE(1004L, "dashboard.delete", MenuCode.DASHBOARD),

    DEVICE_VIEW(2001L, "device.view", MenuCode.DEVICE),
    DEVICE_ADD(2002L, "device.add", MenuCode.DEVICE),
    DEVICE_EDIT(2003L, "device.edit", MenuCode.DEVICE),
    DEVICE_DELETE(2004L, "device.delete", MenuCode.DEVICE),
    DEVICE_GROUP_MANAGE(2005L, "device.group_manage", MenuCode.DEVICE),

    ENTITY_CUSTOM_VIEW(3001L, "entity_custom.view", MenuCode.ENTITY_CUSTOM),
    ENTITY_CUSTOM_ADD(3002L, "entity_custom.add", MenuCode.ENTITY_CUSTOM),
    ENTITY_CUSTOM_EDIT(3003L, "entity_custom.edit", MenuCode.ENTITY_CUSTOM),
    ENTITY_CUSTOM_DELETE(3004L, "entity_custom.delete", MenuCode.ENTITY_CUSTOM),

    ENTITY_DATA_VIEW(4001L, "entity_data.view", MenuCode.ENTITY_DATA),
    ENTITY_DATA_EDIT(4002L, "entity_data.edit", MenuCode.ENTITY_DATA),

    WORKFLOW_VIEW(5001L, "workflow.view", MenuCode.WORKFLOW),
    WORKFLOW_ADD(5002L, "workflow.add", MenuCode.WORKFLOW),
    WORKFLOW_EDIT(5004L, "workflow.edit", MenuCode.WORKFLOW),
    WORKFLOW_DELETE(5006L, "workflow.delete", MenuCode.WORKFLOW),

    INTEGRATION_VIEW(6001L, "integration.view", MenuCode.INTEGRATION),

    CREDENTIALS_VIEW(8001L, "credentials.view", MenuCode.CREDENTIALS),
    CREDENTIALS_EDIT(8002L, "credentials.edit", MenuCode.CREDENTIALS),

    ENTITY_TAG_VIEW(9001L, "entity_tag.view", MenuCode.TAG),
    ENTITY_TAG_MANAGE(9002L, "entity_tag.manage", MenuCode.TAG),
    ;

    private final Long id;
    private final String code;
    private final MenuCode parent;

    @Override
    public MenuType getType() {
        return MenuType.FUNCTION;
    }

}
