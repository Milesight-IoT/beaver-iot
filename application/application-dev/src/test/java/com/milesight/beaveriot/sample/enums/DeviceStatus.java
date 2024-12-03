package com.milesight.beaveriot.sample.enums;

import com.milesight.beaveriot.base.enums.EnumCode;

public enum DeviceStatus implements EnumCode {
    ONLINE("a","online"), OFFLINE("b","offline");

    private String code;
    private String value;
    DeviceStatus(String code, String value) {
        this.code = code;
        this.value = value;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getValue() {
        return value;
    }

}
