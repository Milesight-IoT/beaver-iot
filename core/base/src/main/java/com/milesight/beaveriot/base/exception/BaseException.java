package com.milesight.beaveriot.base.exception;

/**
 * @author leon
 */
public class BaseException extends RuntimeException {

    public BaseException() {
    }

    public BaseException(String message) {
        super(message);
    }

    public BaseException(Throwable throwable) {
        super(throwable);
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
