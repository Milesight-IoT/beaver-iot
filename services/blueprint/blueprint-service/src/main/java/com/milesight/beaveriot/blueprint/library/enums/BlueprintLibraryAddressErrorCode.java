package com.milesight.beaveriot.blueprint.library.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

import java.text.MessageFormat;

/**
 * author: Luxb
 * create: 2025/9/1 10:56
 **/
public enum BlueprintLibraryAddressErrorCode implements ErrorCodeSpec {
    BLUEPRINT_LIBRARY_SOURCE_TYPE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST.value(),
            "blueprint_library_source_type_not_supported", "Blueprint library source type not supported"),
    BLUEPRINT_LIBRARY_ADDRESS_NULL(HttpStatus.BAD_REQUEST.value(),
            "blueprint_library_address_null", "Blueprint library address must not be null"),
    BLUEPRINT_LIBRARY_ADDRESS_URL_EMPTY(HttpStatus.BAD_REQUEST.value(),
            "blueprint_library_address_url_empty", "Blueprint library address url must not be empty"),
    BLUEPRINT_LIBRARY_ADDRESS_URL_INVALID(HttpStatus.BAD_REQUEST.value(),
            "blueprint_library_address_url_invalid", "Blueprint library address url must be a string that matches the pattern ''{0}''"),
    BLUEPRINT_LIBRARY_ADDRESS_BRANCH_EMPTY(HttpStatus.BAD_REQUEST.value(),
            "blueprint_library_address_branch_empty", "Blueprint library address branch must not be empty"),
    BLUEPRINT_LIBRARY_ADDRESS_ACCESS_FAILED(HttpStatus.BAD_REQUEST.value(),
            "blueprint_library_address_access_failed", "Blueprint library address access failed"),
    BLUEPRINT_LIBRARY_ADDRESS_MANIFEST_NOT_REACHABLE(HttpStatus.BAD_REQUEST.value(),
            "blueprint_library_address_manifest_not_reachable", "Blueprint library address manifest is not reachable"),
    BLUEPRINT_LIBRARY_ADDRESS_MANIFEST_INVALID(HttpStatus.BAD_REQUEST.value(),
            "blueprint_library_address_manifest_invalid", "Blueprint library address manifest is invalid"),
    BLUEPRINT_LIBRARY_ADDRESS_BEING_SYNCED(HttpStatus.BAD_REQUEST.value(),
            "blueprint_library_address_being_synced", "Blueprint library address is currently being synchronized. Please try again later.");

    private final String errorCode;
    private final String errorMessage;
    private final int status;

    BlueprintLibraryAddressErrorCode(int status, String errorCode, String errorMessage) {
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
