package com.milesight.beaveriot.base.error;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.MultipleErrorException;
import com.milesight.beaveriot.base.exception.ServiceException;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author leon
 */
@NoArgsConstructor
@Data
public class ErrorHolder {

    /**
     * Exception Code
     */
    private String errorCode;
    /**
     * Exception information
     */
    private String errorMessage;
    /**
     * Exception Data
     */
    private Object args;

    protected ErrorHolder(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.args = null;
    }

    protected ErrorHolder(String errorCode, String errorMessage, Object args) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.args = args;
    }

    public static ErrorHolder of(String errorCode, String errorMessage) {
        return new ErrorHolder(errorCode, errorMessage);
    }

    public static ErrorHolder of(String errorCode, String errorMessage, Object args) {
        return new ErrorHolder(errorCode, errorMessage, args);
    }

    public static List<ErrorHolder> of(List<Throwable> causes) {
        if(ObjectUtils.isEmpty(causes)){
            return Collections.emptyList();
        }
        return causes.stream().map(cause -> {
            String errorCode = ErrorCode.SERVER_ERROR.getErrorCode();
            Object args = null;
            if (cause instanceof ServiceException serviceException) {
                errorCode = serviceException.getErrorCode();
                args = serviceException.getArgs();
            } else if (cause instanceof MultipleErrorException multipleErrorException) {
                errorCode = multipleErrorException.getErrorCode();
                args = multipleErrorException.getErrors();
            }
            return new ErrorHolder(errorCode, cause.getMessage(), args);
        }).toList();
    }
}
