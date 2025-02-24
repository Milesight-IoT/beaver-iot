package com.milesight.beaveriot.base.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leon
 */
public class EventBusExecutionException extends BaseException {

    private final List<Throwable> causes = new ArrayList<>();

    public EventBusExecutionException() {
    }

    public EventBusExecutionException(String message) {
        super(message);
    }

    public EventBusExecutionException(Throwable throwable) {
        super(throwable);
        causes.add(throwable);
    }

    public EventBusExecutionException(String message, Throwable cause) {
        super(message, cause);
        causes.add(cause);
    }

    public EventBusExecutionException(String message, List<Throwable> causes) {
        super(message);
        this.causes.addAll(causes);
    }

    @Override
    public String getMessage() {
        return causes.stream().map(Throwable::getMessage).reduce((a, b) -> a + ", " + b).orElse(super.getMessage());
    }

    public List<Throwable> getCauses() {
        return causes;
    }

}
