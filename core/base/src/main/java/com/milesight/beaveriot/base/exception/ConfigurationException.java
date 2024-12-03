package com.milesight.beaveriot.base.exception;

/**
 * @author leon
 */
public class ConfigurationException extends BaseException {
    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
