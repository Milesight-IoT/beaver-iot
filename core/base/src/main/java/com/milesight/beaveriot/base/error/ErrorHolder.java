package com.milesight.beaveriot.base.error;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import lombok.Data;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author leon
 */
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

    protected ErrorHolder(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static ErrorHolder of(String errorCode, String errorMessage) {
        return new ErrorHolder(errorCode, errorMessage);
    }

    public static List<ErrorHolder> of(List<Throwable> causes) {
        if(ObjectUtils.isEmpty(causes)){
            return Collections.emptyList();
        }
        return causes.stream().map(cause -> {
            String errorCode = cause instanceof ServiceException serviceException? serviceException.getErrorCode() : ErrorCode.SERVER_ERROR.getErrorCode();
            return new ErrorHolder(errorCode, cause.getMessage());
        }).toList();
    }
}
