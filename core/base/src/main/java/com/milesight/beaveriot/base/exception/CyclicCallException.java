package com.milesight.beaveriot.base.exception;

/**
 * @author leon
 */
public class CyclicCallException extends BaseException {
    public CyclicCallException(String message) {
        super(message);
    }

    public CyclicCallException(Throwable throwable) {
        super(throwable);
    }
}
