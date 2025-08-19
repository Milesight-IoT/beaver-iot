package com.milesight.beaveriot.rule.manager.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * WorkflowManagerErrorCode class.
 *
 * @author simon
 * @date 2025/8/15
 */
@Getter
@RequiredArgsConstructor
public enum WorkflowManagerErrorCode implements ErrorCodeSpec {
    WORKFLOW_MISCONFIGURED(HttpStatus.BAD_REQUEST.value()),
    ;

    private final int status;
    private final String errorCode;
    private final String errorMessage;
    private final String detailMessage;

    WorkflowManagerErrorCode() {
        this.status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        this.errorCode = name().toLowerCase();
        this.errorMessage = null;
        this.detailMessage = null;
    }

    WorkflowManagerErrorCode(int status) {
        this.status = status;
        this.errorCode = name().toLowerCase();
        this.errorMessage = null;
        this.detailMessage = null;
    }

    WorkflowManagerErrorCode(int status, String errorMessage, String detailMessage) {
        this.status = status;
        this.errorCode = name().toLowerCase();
        this.errorMessage = errorMessage;
        this.detailMessage = detailMessage;
    }
}
