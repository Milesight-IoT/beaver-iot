package com.milesight.beaveriot.base.error;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.milesight.beaveriot.base.exception.BaseException;
import com.milesight.beaveriot.base.exception.EventBusExecutionException;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.base.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.BindException;
import java.util.ArrayList;
import java.util.List;

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
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public com.milesight.beaveriot.base.response.ResponseBody<Object> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        log.error("Cause MethodArgumentNotValidException: ", e);
        List<ObjectError> objectErrors = extractBindingErrors(e);
        return CollectionUtils.isEmpty(objectErrors) ?
                ResponseBuilder.fail(ErrorCode.PARAMETER_VALIDATION_FAILED, e.getMessage()) :
                ResponseBuilder.fail(ErrorCode.PARAMETER_VALIDATION_FAILED, objectErrors);
    }

    @ResponseBody
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public com.milesight.beaveriot.base.response.ResponseBody<Object> bindExceptionHandler(BindException e) {
        log.debug("Cause BindException Detail:", e);
        List<ObjectError> objectErrors = extractBindingErrors(e);
        return CollectionUtils.isEmpty(objectErrors) ?
                ResponseBuilder.fail(ErrorCode.PARAMETER_VALIDATION_FAILED, e.getMessage()) :
                ResponseBuilder.fail(ErrorCode.PARAMETER_VALIDATION_FAILED, objectErrors);
    }

    @ResponseBody
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Object> serviceExceptionHandler(ServiceException e) {
        log.debug("Cause ServiceException Detail:", e);
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

    @ResponseBody
    @ExceptionHandler({CamelExecutionException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Object> handleException(CamelExecutionException e) {
        log.error("Cause CamelExecutionException {}", e.getMessage());
        Throwable cause = e.getCause();
        if (cause != null) {
            if (cause instanceof ServiceException serviceException) {
                return ResponseEntity.status(serviceException.getStatus()).body(ResponseBuilder.fail(serviceException));
            } else if (cause instanceof EventBusExecutionException eventBusExecutionException) {
                return ResponseEntity.status(ErrorCode.SERVER_ERROR.getStatus()).body(ResponseBuilder.fail(ErrorCode.SERVER_ERROR.getErrorCode(), cause.getMessage(), null, ErrorHolder.of(eventBusExecutionException.getCauses())));
            } else if (cause instanceof BaseException) {
                return ResponseEntity.status(ErrorCode.SERVER_ERROR.getStatus()).body(ResponseBuilder.fail(ErrorCode.SERVER_ERROR.getErrorCode(), cause.getMessage()));
            }
        }
        return ResponseEntity.status(ErrorCode.SERVER_ERROR.getStatus()).body(ResponseBuilder.fail(ErrorCode.SERVER_ERROR, cause.getMessage()));

    }

    @ResponseBody
    @ExceptionHandler({RuntimeException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public com.milesight.beaveriot.base.response.ResponseBody<Object> handleException(RuntimeException e) {
        log.error("Cause RuntimeException :", e);
        return ResponseBuilder.fail(ErrorCode.SERVER_ERROR, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler({Throwable.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public com.milesight.beaveriot.base.response.ResponseBody<Object> handleException(Throwable e) {
        log.error("Cause Throwable : ", e);
        return ResponseBuilder.fail(ErrorCode.SERVER_ERROR, e.getMessage());
    }

    private BindingResult extractBindingResult(Throwable error) {
        if (error instanceof BindingResult bindingResult) {
            return bindingResult;
        }
        if (error instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            return methodArgumentNotValidException.getBindingResult();
        }
        return null;
    }

    private List<ObjectError> extractBindingErrors(Throwable error) {
        BindingResult result = extractBindingResult(error);
        if (result != null && result.hasErrors()) {
            return result.getAllErrors();
        }
        return new ArrayList<>();
    }

}
