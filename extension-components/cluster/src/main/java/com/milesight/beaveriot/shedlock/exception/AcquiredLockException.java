package com.milesight.beaveriot.shedlock.exception;

import com.milesight.beaveriot.base.exception.BaseException;

/**
 * @author leon
 */
public class AcquiredLockException extends BaseException {

    public AcquiredLockException(String message) {
        super(message);
    }

    public AcquiredLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
