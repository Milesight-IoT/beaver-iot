package com.milesight.beaveriot.blueprint.core.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BlueprintErrorCode implements ErrorCodeSpec {
    BLUEPRINT_PARAMETERS_VALIDATION_ERROR("Validation of blueprint input parameters failed"),
    BLUEPRINT_TEMPLATE_PARSING_FAILED("Failed to parse blueprint template"),
    BLUEPRINT_MAX_TEMPLATE_NODE_COUNT_EXCEED("Maximum blueprint template node count exceeded"),
    BLUEPRINT_MAX_NESTED_TEMPLATE_COUNT_EXCEED("Maximum number of nested blueprint templates exceeded"),
    BLUEPRINT_FUNCTION_EXECUTOR_NOT_FOUND("Blueprint function executor not found"),
    BLUEPRINT_FUNCTION_EXECUTION_FAILED("Blueprint function execution failed"),
    BLUEPRINT_RESOURCE_MANAGER_NOT_FOUND("Blueprint resource manager not found"),
    BLUEPRINT_RESOURCE_DEPLOYMENT_FAILED("Blueprint resource deployment failed"),
    BLUEPRINT_CIRCULAR_DEPENDENCY_DETECTED("Circular dependency detected in blueprint"),
    ;

    private final String errorCode = name().toLowerCase();

    private String errorMessage;

    private String detailMessage;

    BlueprintErrorCode(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    BlueprintErrorCode(String errorMessage, String detailMessage) {
        this.errorMessage = errorMessage;
        this.detailMessage = detailMessage;
    }

    @Override
    public String toString() {
        return name();
    }

}
