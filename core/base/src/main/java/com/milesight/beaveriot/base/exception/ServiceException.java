package com.milesight.beaveriot.base.exception;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

/**
 * @author leon
 */
public class ServiceException extends BaseException {

    /**
     * Http response status code, default 500
     */
    private int status;
    /**
     * Exception Code
     */
    private String errorCode;
    /**
     * Exception information
     */
    private String errorMessage;

    /**
     * Detailed exception information
     */
    private String detailMessage;
    /**
     * The parameters corresponding to the error code can be responded to the front end
     */
    private transient Object args = null;

    public ServiceException(int status, String code, String message, String detailMessage, Object args, Throwable throwable) {
        super(StringUtils.hasText(message) ? message : code, throwable);
        this.status = status;
        this.errorCode = code;
        this.errorMessage = message;
        this.detailMessage = detailMessage;
        this.args = args;
    }

    public ServiceException() {
    }

    public ServiceException(String errorCode, String errorMessage) {
        this(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorCode, errorMessage, null, null, null);
    }

    public ServiceException(String errorCode, String errorMessage, Object args) {
        this(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorCode, errorMessage, null, args, null);
    }

    public ServiceException(String errorCode, String errorMessage, Throwable throwable) {
        this(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorCode, errorMessage, null, null, throwable);
    }

    public ServiceException(String errorCode, String errorMessage, Object args, Throwable throwable) {
        this(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorCode, errorMessage, null, args, throwable);
    }

    public ServiceException(ErrorCodeSpec errorCodeSpec) {
        this(errorCodeSpec, null, null, null);
    }

    public ServiceException(ErrorCodeSpec errorCodeSpec, String detailMessage) {
        this(errorCodeSpec, detailMessage, null, null);
    }

    public ServiceException(ErrorCodeSpec errorCodeSpec, String detailMessage, Object args) {
        this(errorCodeSpec, detailMessage, args, null);
    }

    public ServiceException(ErrorCodeSpec errorCodeSpec, Throwable throwable) {
        this(errorCodeSpec, null, null, throwable);
    }

    public ServiceException(ErrorCodeSpec errorCodeSpec, String detailMessage, Throwable throwable) {
        this(errorCodeSpec, detailMessage, null, throwable);
    }

    public ServiceException(ErrorCodeSpec errorCodeSpec, Object args, Throwable throwable) {
        this(errorCodeSpec, errorCodeSpec.getDetailMessage(), args, throwable);
    }

    public ServiceException(ErrorCodeSpec errorCodeSpec, String detailMessage, Object args, Throwable throwable) {
        this(errorCodeSpec.getStatus(), errorCodeSpec.getErrorCode(), errorCodeSpec.getErrorMessage(), detailMessage, args, throwable);
    }

    public int getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String getMessage() {
        return StringUtils.hasText(detailMessage) ? super.getMessage() + ": " + detailMessage : super.getMessage();
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public void setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
    }

    public Object getArgs() {
        return args;
    }

    public ServiceException detailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }

    public ServiceException args(Object data) {
        this.args = data;
        return this;
    }

    public static ServiceExceptionBuilder with(ErrorCodeSpec errorCodeSpec) {
        return new ServiceExceptionBuilder(errorCodeSpec.getErrorCode(), errorCodeSpec.getErrorMessage()).status(errorCodeSpec.getStatus());
    }

    public static ServiceExceptionBuilder with(String errorCode, String errorMessage) {
        return new ServiceExceptionBuilder(errorCode, errorMessage);
    }

    public static class ServiceExceptionBuilder {
        private int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        private String errorCode;
        private String errorMessage;
        private String detailMessage;
        private Object args = null;
        private Throwable throwable;

        public ServiceExceptionBuilder(String errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public ServiceExceptionBuilder status(int status) {
            this.status = status;
            return this;
        }

        public ServiceExceptionBuilder detailMessage(String detailMessage) {
            this.detailMessage = detailMessage;
            return this;
        }

        public ServiceExceptionBuilder args(Object args) {
            this.args = args;
            return this;
        }

        public ServiceExceptionBuilder throwable(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public ServiceException build() {
            return new ServiceException(status, errorCode, errorMessage, detailMessage, args, throwable);
        }
    }

}
