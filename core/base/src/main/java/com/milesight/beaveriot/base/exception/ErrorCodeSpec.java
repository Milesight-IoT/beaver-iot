package com.milesight.beaveriot.base.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception error code specification interface, developers can implement exception enumeration error codes and implement this interface
 *
 * @author leon
 */
public interface ErrorCodeSpec {

    /**
     * Http status code, default 500
     *
     * @return
     */
    default int getStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    /**
     * exception error code
     *
     * @return
     */
    String getErrorCode();

    /**
     * exception message
     *
     * @return
     */
    String getErrorMessage();

    String getDetailMessage();
}
