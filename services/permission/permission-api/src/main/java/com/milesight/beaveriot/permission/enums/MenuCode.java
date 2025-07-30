package com.milesight.beaveriot.permission.enums;

import lombok.*;

/**
 * @author loong
 */
@AllArgsConstructor
@Getter
public enum MenuCode implements MenuItem {

    DASHBOARD(1000L),
    DEVICE(2000L),
    ENTITY(7000L),
    ENTITY_CUSTOM(3000L, ENTITY),
    ENTITY_DATA(4000L, ENTITY),
    WORKFLOW(5000L),
    INTEGRATION(6000L),
    SETTINGS(7900L),
    CREDENTIALS(8000L, SETTINGS),
    TAG(9000L),
    ;

    private final Long id;
    private final String code;
    private final MenuCode parent;

    MenuCode(Long id) {
        this(id, null);
    }

    MenuCode(Long id, MenuCode parent) {
        this.id = id;
        this.code = name().toLowerCase();
        this.parent = parent;
    }

    @Override
    public MenuType getType() {
        return MenuType.MENU;
    }

}
