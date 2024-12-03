package com.milesight.beaveriot.entity.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author loong
 * @date 2024/10/16 17:42
 */
@RequiredArgsConstructor
@Getter
public enum EntityErrorCode implements ErrorCodeSpec {


    ;

    private final String errorCode;
    private final String errorMessage;
    private final String detailMessage;

    EntityErrorCode() {
        this.errorCode = name().toLowerCase();
        this.errorMessage = null;
        this.detailMessage = null;
    }

    EntityErrorCode(String errorMessage) {
        this.errorCode = name().toLowerCase();
        this.errorMessage = errorMessage;
        this.detailMessage = null;
    }

    EntityErrorCode(String errorMessage, String detailMessage) {
        this.errorCode = name().toLowerCase();
        this.errorMessage = errorMessage;
        this.detailMessage = detailMessage;
    }

}
