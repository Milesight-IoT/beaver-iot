package com.milesight.beaveriot.rule.exception;

/**
 * @author leon
 */
public class RuleEngineException extends RuntimeException {

    public RuleEngineException(String message) {
        super(message);
    }

    public RuleEngineException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuleEngineException(Throwable cause) {
        super(cause);
    }
}
