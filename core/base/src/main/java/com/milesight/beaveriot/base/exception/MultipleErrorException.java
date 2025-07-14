package com.milesight.beaveriot.base.exception;

import com.milesight.beaveriot.base.error.ErrorHolderExt;
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
    private int status;
    private final List<ErrorHolderExt> errors;

    private MultipleErrorException(int status, String message, List<ErrorHolderExt> errors) {
        super(message);
        this.status = status;
        this.errors = errors;
    }

    public static MultipleErrorException with(String message, List<ErrorHolderExt> errors) {
        return with(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, errors);
    }

    public static MultipleErrorException with(int status, String message, List<ErrorHolderExt> errors) {
        return new MultipleErrorException(status, message, errors);
    }
}