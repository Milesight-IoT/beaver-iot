package com.milesight.beaveriot.user.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author loong
 * @date 2024/10/16 17:42
 */
@RequiredArgsConstructor
@Getter
public enum UserErrorCode implements ErrorCodeSpec {

    USER_REGISTER_EMAIL_EXIST,
    ;

    private final String errorCode;
    private final String errorMessage;
    private final String detailMessage;

    UserErrorCode() {
        this.errorCode = name().toLowerCase();
        this.errorMessage = null;
        this.detailMessage = null;
    }

    UserErrorCode(String errorMessage) {
        this.errorCode = name().toLowerCase();
        this.errorMessage = errorMessage;
        this.detailMessage = null;
    }

    UserErrorCode(String errorMessage, String detailMessage) {
        this.errorCode = name().toLowerCase();
        this.errorMessage = errorMessage;
        this.detailMessage = detailMessage;
    }

}
