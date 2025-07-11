package com.milesight.beaveriot.permission.enums;

public interface MenuItem {

    Long getId();

    String getCode();

    MenuItem getParent();

    MenuType getType();

}
