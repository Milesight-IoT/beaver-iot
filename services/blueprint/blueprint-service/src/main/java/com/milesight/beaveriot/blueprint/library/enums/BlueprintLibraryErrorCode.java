package com.milesight.beaveriot.blueprint.library.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

import java.text.MessageFormat;

public enum BlueprintLibraryErrorCode implements ErrorCodeSpec {
    BLUEPRINT_LIBRARY_NULL(HttpStatus.BAD_REQUEST.value(),
            "blueprint_library_null", "Blueprint library must not be null"),
    BLUEPRINT_LIBRARY_BEAVER_VERSION_UNSUPPORTED(HttpStatus.BAD_REQUEST.value(),
            "blueprint_library_beaver_version_unsupported", "Blueprint library beaver version unsupported"),
    BLUEPRINT_LIBRARY_ZIP_URL_EMPTY(HttpStatus.BAD_REQUEST.value(),
            "blueprint_library_zip_url_empty", "Blueprint library zip url must not be empty"),
    BLUEPRINT_LIBRARY_RESOURCES_FETCH_FAILED(HttpStatus.BAD_REQUEST.value(),
            "blueprint_library_resources_fetch_failed", "Blueprint library resources fetch failed"),
    BLUEPRINT_LIBRARY_DEFAULT_NOT_FOUND(HttpStatus.NOT_FOUND.value(),
            "blueprint_library_default_not_found", "Blueprint library default not found");
    private final String errorCode;
    private final String errorMessage;
    private final int status;

    BlueprintLibraryErrorCode(int status, String errorCode, String errorMessage) {
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
