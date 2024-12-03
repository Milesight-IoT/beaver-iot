package com.milesight.beaveriot.base.exception;

/**
 * @author leon
 */
public class JSONException extends BaseException {
    public JSONException(String message) {
        super(message);
    }

    public JSONException(String message, Throwable cause) {
        super(message, cause);
    }

    public JSONException(Throwable cause) {
        super(cause);
    }

}
