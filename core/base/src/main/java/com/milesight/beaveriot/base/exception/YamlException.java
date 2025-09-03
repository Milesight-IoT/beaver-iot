package com.milesight.beaveriot.base.exception;


public class YamlException extends BaseException {
    public YamlException(String message) {
        super(message);
    }

    public YamlException(String message, Throwable cause) {
        super(message, cause);
    }

    public YamlException(Throwable cause) {
        super(cause);
    }

}
