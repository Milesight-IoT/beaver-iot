package com.milesight.beaveriot.permission.enums;

import lombok.*;

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
    CREDENTIALS("credentials", null),
    ;
    private final String code;
    private final MenuCode parent;
}
