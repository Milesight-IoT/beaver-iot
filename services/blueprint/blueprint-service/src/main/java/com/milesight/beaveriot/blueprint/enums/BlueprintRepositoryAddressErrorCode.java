package com.milesight.beaveriot.blueprint.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

import java.text.MessageFormat;

/**
 * author: Luxb
 * create: 2025/9/1 10:56
 **/
public enum BlueprintRepositoryAddressErrorCode implements ErrorCodeSpec {
    BLUEPRINT_REPOSITORY_ADDRESS_NULL(HttpStatus.BAD_REQUEST.value(),
            "blueprint_repository_address_null", "Blueprint repository address must not be null"),
    BLUEPRINT_REPOSITORY_ADDRESS_HOME_EMPTY(HttpStatus.BAD_REQUEST.value(),
            "blueprint_repository_address_home_empty", "Blueprint repository address home must not be empty"),
    BLUEPRINT_REPOSITORY_ADDRESS_HOME_INVALID(HttpStatus.BAD_REQUEST.value(),
            "blueprint_repository_address_home_invalid", "Blueprint repository address home must be a string that matches the pattern {0}"),
    BLUEPRINT_REPOSITORY_ADDRESS_BRANCH_EMPTY(HttpStatus.BAD_REQUEST.value(),
            "blueprint_repository_address_branch_empty", "Blueprint repository address branch must not be empty"),
    BLUEPRINT_REPOSITORY_ADDRESS_BRANCH_INVALID(HttpStatus.BAD_REQUEST.value(),
            "blueprint_repository_address_branch_invalid", "Blueprint repository address branch must be a string that matches the pattern {0}"),
    BLUEPRINT_REPOSITORY_ADDRESS_MANIFEST_NOT_REACHABLE(HttpStatus.BAD_REQUEST.value(),
            "blueprint_repository_address_manifest_not_reachable", "Blueprint repository address manifest is not reachable"),
    BLUEPRINT_REPOSITORY_ADDRESS_MANIFEST_INVALID(HttpStatus.BAD_REQUEST.value(),
            "blueprint_repository_address_manifest_invalid", "Blueprint repository address manifest is invalid");

    private final String errorCode;
    private final String errorMessage;
    private final int status;

    BlueprintRepositoryAddressErrorCode(int status, String errorCode, String errorMessage) {
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
