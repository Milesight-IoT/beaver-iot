package com.milesight.beaveriot.base.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

/**
 * @author leon
 */
public enum ErrorCode implements ErrorCodeSpec {
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "server_error"),
    PARAMETER_VALIDATION_FAILED(HttpStatus.BAD_REQUEST.value(), "parameter_validation_failed"),
    REQUEST_TOO_FREQUENTLY(HttpStatus.TOO_MANY_REQUESTS.value(), "request_too_frequently"),
    PARAMETER_SYNTAX_ERROR(HttpStatus.BAD_REQUEST.value(), "parameter_syntax_error"),
    DATA_NO_FOUND(HttpStatus.NOT_FOUND.value(), "data_no_found"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED.value(), "token_expired"),
    FORBIDDEN_PERMISSION(HttpStatus.FORBIDDEN.value(), "forbidden_permission"),
    NO_DATA_PERMISSION(HttpStatus.FORBIDDEN.value(), "no_data_permission"),
    METHOD_NOT_ALLOWED(HttpStatus.BAD_REQUEST.value(), "method_not_allowed"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED.value(), "token_invalid"),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED.value(), "authentication_failed"),
    DATA_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE.value(), "data_too_large"),
    EVENTBUS_EXECUTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "eventbus_execution_error"),
    MULTIPLE_ERROR(HttpStatus.BAD_REQUEST.value(), "multiple_error"),
    ;

    private String errorCode;
    private String errorMessage;
    private String detailMessage;
    private int status = HttpStatus.INTERNAL_SERVER_ERROR.value();

    ErrorCode(int status, String errorCode, String errorMessage, String detailMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.status = status;
        this.detailMessage = detailMessage;
    }

    ErrorCode(int status, String errorCode) {
        this.errorCode = errorCode;
        this.status = status;
    }

    ErrorCode(int status, String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.status = status;
    }

    ErrorCode(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    ErrorCode(String errorCode) {
        this.errorCode = errorCode;
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
        return detailMessage;
    }

    @Override
    public int getStatus() {
        return status;
    }
}
