package com.milesight.beaveriot.context.api;

import java.time.Duration;

/**
 * Provider interface for scheduling delayed task execution.
 * <p>
 * This interface defines a contract for executing tasks after a specified delay.
 * Implementations typically use distributed delayed queues to support task scheduling
 * in clustered environments.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * DelayedSchedulerProvider scheduler = ...;
 * scheduler.schedule(Duration.ofMinutes(5), () -> {
 *     System.out.println("Task executed after 5 minutes");
 * });
 * }</pre>
 *
 * @author Luxb
 * @date 2025/11/26 10:31
 */
public interface DelayedSchedulerProvider {
    /**
     * Schedules a task to be executed after the specified delay.
     * <p>
     * The task will be submitted to a delayed queue and executed once the delay period
     * has elapsed. This method returns immediately without waiting for the task execution.
     * </p>
     *
     * @param delay the time duration to wait before executing the task, must not be null
     * @param runnable the task to execute after the delay, must not be null
     * @throws IllegalArgumentException if delay or runnable is null
     */
    void schedule(Duration delay, Runnable runnable);
}