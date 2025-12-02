package com.milesight.beaveriot.coalescer;

import lombok.SneakyThrows;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Request Coalescer (Single Flight Pattern)
 * <p>
 * Deduplicates concurrent requests with the same key. Only the first request executes,
 * while others wait and share the result.
 * </p>
 * <p>
 * This is useful for scenarios like device status updates where multiple concurrent
 * requests with identical parameters should be coalesced to avoid lock contention.
 * </p>
 *
 * @param <V> Result type
 * @author simon
 */
public interface RequestCoalescer<V> {

    /**
     * Execute a task with request coalescing.
     * <p>
     * If another request with the same key is already in-flight, this method
     * returns a CompletableFuture that will complete with the same result.
     * Otherwise, it executes the task and stores the result for concurrent requests.
     * </p>
     *
     * @param key  Request key for deduplication
     * @param task Task to execute if no in-flight request exists
     * @return CompletableFuture with the result
     */
    @SneakyThrows
    default V execute(String key, Supplier<V> task) {
        return executeAsync(key, task).get();
    }

    /**
     * Execute an async task with request coalescing.
     *
     * @param key  Request key for deduplication
     * @param task Async task to execute if no in-flight request exists
     * @return CompletableFuture with the result
     */
    CompletableFuture<V> executeAsync(String key, Supplier<V> task);
}
