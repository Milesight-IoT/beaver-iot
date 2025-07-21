package com.milesight.beaveriot.base.exception;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.error.ErrorHolder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/7/11 16:47
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class MultipleErrorException extends BaseException {
    private String errorCode = ErrorCode.MULTIPLE_ERROR.getErrorCode();
    private int status;
    private final List<ErrorHolder> errors;

    private MultipleErrorException(int status, String message, List<ErrorHolder> errors) {
        super(message);
        this.status = status;
        this.errors = errors;
    }

    public static MultipleErrorException with(String message, List<ErrorHolder> errors) {
        return with(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, errors);
    }

    public static MultipleErrorException with(int status, String message, List<ErrorHolder> errors) {
        return new MultipleErrorException(status, message, errors);
    }
}