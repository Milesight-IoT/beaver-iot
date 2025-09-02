package com.milesight.beaveriot.blueprint.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

import java.text.MessageFormat;

public enum BlueprintRepositoryErrorCode implements ErrorCodeSpec {
    BLUEPRINT_REPOSITORY_BEAVER_VERSION_UNSUPPORTED(HttpStatus.BAD_REQUEST.value(),
            "blueprint_repository_beaver_version_unsupported", "Blueprint repository beaver version unsupported"),
    BLUEPRINT_REPOSITORY_SYNC_FAILED(HttpStatus.BAD_REQUEST.value(),
            "blueprint_repository_sync_failed", "Blueprint repository sync failed");
    private final String errorCode;
    private final String errorMessage;
    private final int status;

    BlueprintRepositoryErrorCode(int status, String errorCode, String errorMessage) {
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String formatMessage(Object... args) {
        return MessageFormat.format(errorMessage, args);
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String getDetailMessage() {
        return null;
    }
}
