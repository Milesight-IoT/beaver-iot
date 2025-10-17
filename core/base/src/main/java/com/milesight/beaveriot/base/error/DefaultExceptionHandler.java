package com.milesight.beaveriot.base.error;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.BaseException;
import com.milesight.beaveriot.base.exception.EventBusExecutionException;
import com.milesight.beaveriot.base.exception.MultipleErrorException;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import lombok.extern.slf4j.*;
import org.apache.camel.CamelExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * @author leon
 */
@ControllerAdvice
@Slf4j
public class DefaultExceptionHandler {

    @ResponseBody
    @ExceptionHandler(InvalidFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public com.milesight.beaveriot.base.response.ResponseBody<Object> invalidFormatException(InvalidFormatException e) {
        log.error("Cause invalidFormatException: ", e);
        return ResponseBuilder.fail(ErrorCode.PARAMETER_VALIDATION_FAILED);
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public com.milesight.beaveriot.base.response.ResponseBody<Object> methodArgumentTypeMismatchExceptionHandler(MethodArgumentTypeMismatchException e) {
        log.error("Cause MethodArgumentTypeMismatchException:", e);
        return ResponseBuilder.fail(ErrorCode.PARAMETER_SYNTAX_ERROR);
    }

    @ResponseBody
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public com.milesight.beaveriot.base.response.ResponseBody<Object> methodArgumentNotValidExceptionHandler(BindException e) {
        log.error("Cause BindException: ", e);
        return BindingResultResolver.convertToErrorResponse(e);
    }

    @ResponseBody
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Object> serviceExceptionHandler(ServiceException e) {
        log.error("Cause ServiceException Detail:", e);
        return ResponseEntity.status(e.getStatus()).body(ResponseBuilder.fail(e));
    }

    @ResponseBody
    @ExceptionHandler(BaseException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public com.milesight.beaveriot.base.response.ResponseBody<Object> baseExceptionHandler(BaseException e) {
        log.error("Cause BaseException:", e);
        return ResponseBuilder.fail(ErrorCode.SERVER_ERROR);
    }

    @ResponseBody
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public com.milesight.beaveriot.base.response.ResponseBody<Object> illegalArgumentExceptionHandler(IllegalArgumentException e) {
        log.error("Cause IllegalArgumentException:", e);
        return ResponseBuilder.fail(ErrorCode.PARAMETER_VALIDATION_FAILED, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public com.milesight.beaveriot.base.response.ResponseBody<Object> handleException(Exception e) {
        log.error("Cause Exception:", e);
        return ResponseBuilder.fail(ErrorCode.SERVER_ERROR, e.getMessage());
    }

    private ResponseEntity<Object> getErrorResponse(Throwable cause) {
        if (cause != null) {
            cause = ErrorHolder.tryGetKnownCause(cause);
            if (cause instanceof ServiceException serviceException) {
                return ResponseEntity.status(serviceException.getStatus()).body(ResponseBuilder.fail(serviceException));
            } else if (cause instanceof EventBusExecutionException eventBusExecutionException) {
                return ResponseEntity.status(ErrorCode.SERVER_ERROR.getStatus()).body(ResponseBuilder.fail(ErrorCode.EVENTBUS_EXECUTION_ERROR.getErrorCode(), cause.getMessage(), null, ErrorHolder.of(eventBusExecutionException.getCauses())));
            } else if (cause instanceof MultipleErrorException multipleErrorException) {
                return ResponseEntity.status(ErrorCode.SERVER_ERROR.getStatus()).body(ResponseBuilder.fail(ErrorCode.MULTIPLE_ERROR.getErrorCode(), cause.getMessage(), null, multipleErrorException.getErrors()));
            } else if (cause instanceof BaseException) {
                return ResponseEntity.status(ErrorCode.SERVER_ERROR.getStatus()).body(ResponseBuilder.fail(ErrorCode.SERVER_ERROR.getErrorCode(), cause.getMessage()));
            }

            return ResponseEntity.status(ErrorCode.SERVER_ERROR.getStatus()).body(ResponseBuilder.fail(ErrorCode.SERVER_ERROR, cause.getMessage()));
        }

        return ResponseEntity.status(ErrorCode.SERVER_ERROR.getStatus()).body(ResponseBuilder.fail(ErrorCode.SERVER_ERROR));
    }

    @ResponseBody
    @ExceptionHandler({CamelExecutionException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Object> handleCamelException(CamelExecutionException e) {
        log.error("Cause CamelExecutionException {}", e.getMessage());
        Throwable cause = e.getCause();
        return getErrorResponse(cause);
    }

    @ResponseBody
    @ExceptionHandler(MultipleErrorException.class)
    public ResponseEntity<Object> multipleErrorExceptionHandler(MultipleErrorException e) {
        log.debug("Cause MultipleErrorException Detail:", e);
        return ResponseEntity.status(e.getStatus()).body(ResponseBuilder.fail(ErrorCode.MULTIPLE_ERROR.getErrorCode(), e.getMessage(), null, e.getErrors()));
    }

    @ResponseBody
    @ExceptionHandler({RuntimeException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Object> handleException(RuntimeException e) {
        log.error("Cause RuntimeException :", e);
        return getErrorResponse(e);
    }

    @ResponseBody
    @ExceptionHandler({Throwable.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public com.milesight.beaveriot.base.response.ResponseBody<Object> handleException(Throwable e) {
        log.error("Cause Throwable : ", e);
        return ResponseBuilder.fail(ErrorCode.SERVER_ERROR, e.getMessage());
    }

}
