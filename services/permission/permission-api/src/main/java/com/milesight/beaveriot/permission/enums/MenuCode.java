package com.milesight.beaveriot.permission.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author loong
 */
@AllArgsConstructor
@Getter
public enum MenuCode {

    DASHBOARD("dashboard", null),
    DEVICE("device", null),
    ENTITY("entity", null),
    ENTITY_CUSTOM("entity_custom", ENTITY),
    ENTITY_DATA("entity_data", ENTITY),
    WORKFLOW("workflow", null),
    INTEGRATION("integration", null),
    ;
    private final String code;
    private final MenuCode parent;
}
