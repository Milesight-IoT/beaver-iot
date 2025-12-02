package com.milesight.beaveriot.coalescer.redis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private static final long serialVersionUID = 1L;

    /**
     * Execution status: success or error
     */
    private Status status;

    /**
     * Result value (when status is SUCCESS)
     */
    private V value;

    /**
     * Error message (when status is ERROR)
     */
    private String errorMessage;

    /**
     * Error class name (when status is ERROR)
     */
    private String errorClass;

    /**
     * Task execution status
     */
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

    /**
     * Create a successful result.
     *
     * @param value Result value
     * @param <V>   Value type
     * @return TaskResult
     */
    public static <V> TaskResult<V> success(V value) {
        return new TaskResult<>(Status.SUCCESS, value, null, null);
    }

    /**
     * Create an error result.
     *
     * @param throwable Exception
     * @param <V>       Value type
     * @return TaskResult
     */
    public static <V> TaskResult<V> error(Throwable throwable) {
        return new TaskResult<>(
                Status.ERROR,
                null,
                throwable.getMessage(),
                throwable.getClass().getName()
        );
    }

    /**
     * Check if this is a successful result.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Check if this is an error result.
     *
     * @return true if error
     */
    public boolean isError() {
        return status == Status.ERROR;
    }
}
