package com.milesight.beaveriot.blueprint.library.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

import java.text.MessageFormat;

public enum BlueprintLibraryResourceErrorCode implements ErrorCodeSpec {
    BLUEPRINT_LIBRARY_RESOURCE_MANIFEST_NOT_FOUND(HttpStatus.NOT_FOUND.value(),
            "blueprint_library_resource_manifest_not_found", "Blueprint library resource manifest not found"),
    BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDORS_NOT_FOUND(HttpStatus.NOT_FOUND.value(),
            "blueprint_library_resource_device_vendors_not_found", "Blueprint library resource device vendors not found"),
    BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NOT_FOUND(HttpStatus.NOT_FOUND.value(),
            "blueprint_library_resource_device_vendor_not_found", "Blueprint library resource device vendor ''{0}'' not found"),
    BLUEPRINT_LIBRARY_RESOURCE_DEVICE_VENDOR_NULL(HttpStatus.BAD_REQUEST.value(),
            "blueprint_library_resource_device_vendor_null", "Blueprint library resource device vendor must not be null"),
    BLUEPRINT_LIBRARY_RESOURCE_DEVICE_MODEL_NULL(HttpStatus.BAD_REQUEST.value(),
            "blueprint_library_resource_device_model_null", "Blueprint library resource device model must not be null"),
    BLUEPRINT_LIBRARY_RESOURCE_DEVICE_MODELS_NOT_FOUND(HttpStatus.NOT_FOUND.value(),
            "blueprint_library_resource_device_models_not_found", "Blueprint library resource device models not found for vendor ''{0}''"),
    BLUEPRINT_LIBRARY_RESOURCE_DEVICE_MODEL_NOT_FOUND(HttpStatus.NOT_FOUND.value(),
            "blueprint_library_resource_device_model_not_found", "Blueprint library resource device model not found for vendor ''{0}'' and model ''{1}''"),
    BLUEPRINT_LIBRARY_RESOURCE_DEVICE_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND.value(),
            "blueprint_library_resource_device_template_not_found", "Blueprint library resource device template not found for vendor ''{0}'' and model ''{1}''"),
    BLUEPRINT_LIBRARY_RESOURCE_DEVICE_CODEC_NOT_FOUND(HttpStatus.NOT_FOUND.value(),
            "blueprint_library_resource_device_codec_not_found", "Blueprint library resource device codec not found");
    private final String errorCode;
    private final String errorMessage;
    private final int status;

    BlueprintLibraryResourceErrorCode(int status, String errorCode, String errorMessage) {
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
