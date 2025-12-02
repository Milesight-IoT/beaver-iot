package com.milesight.beaveriot.coalescer.redis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Wrapper for storing task execution result in Redis.
 * <p>
 * Supports both successful results and error information.
 * </p>
 *
 * @param <V> Result value type
 * @author simon
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResult<V> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String key;

    private Status status;

    private V value;

    private String errorMessage;

    private String errorClass;

    public enum Status {
        /**
         * Task completed successfully
         */
        SUCCESS,

        /**
         * Task failed with error
         */
        ERROR
    }

    public static <V> TaskResult<V> success(String key, V value) {
        return new TaskResult<>(key, Status.SUCCESS, value, null, null);
    }

    public static <V> TaskResult<V> error(String key, Throwable throwable) {
        return new TaskResult<>(
                key,
                Status.ERROR,
                null,
                throwable.getMessage(),
                throwable.getClass().getName()
        );
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }
}
