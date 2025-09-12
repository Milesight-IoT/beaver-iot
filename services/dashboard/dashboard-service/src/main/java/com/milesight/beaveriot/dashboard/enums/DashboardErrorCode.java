package com.milesight.beaveriot.dashboard.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author loong
 * @date 2024/10/16 17:43
 */
@RequiredArgsConstructor
@Getter
public enum DashboardErrorCode implements ErrorCodeSpec {

    DASHBOARD_NAME_EXIST,
    DASHBOARD_PRESET_COVER_NOT_EXIST,
    ;

    private final String errorCode;
    private final String errorMessage;
    private final String detailMessage;

    DashboardErrorCode() {
        this.errorCode = name().toLowerCase();
        this.errorMessage = null;
        this.detailMessage = null;
    }

    DashboardErrorCode(String errorMessage) {
        this.errorCode = name().toLowerCase();
        this.errorMessage = errorMessage;
        this.detailMessage = null;
    }

    DashboardErrorCode(String errorMessage, String detailMessage) {
        this.errorCode = name().toLowerCase();
        this.errorMessage = errorMessage;
        this.detailMessage = detailMessage;
    }

}
