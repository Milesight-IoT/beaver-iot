package com.milesight.beaveriot.coalescer;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * In-memory Request Coalescer implementation.
 * @param <V> Result type
 * @author simon
 */
@Slf4j
public class InMemoryRequestCoalescer<V> implements RequestCoalescer<V> {


    private final Executor executor;

    public InMemoryRequestCoalescer(Executor executor) {
        this.executor = executor;
        log.debug("Created InMemoryRequestCoalescer instance");
    }

    protected final ConcurrentMap<String, CompletableFuture<V>> inflightRequests = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<V> executeAsync(String key, Supplier<V> task) {
        CompletableFuture<V> existingFuture = inflightRequests.get(key);
        if (existingFuture != null) {
            log.debug("Request coalesced for key: {}", key);
            return existingFuture;
        }

        return inflightRequests.computeIfAbsent(key, k -> {
            CompletableFuture<V> newFuture = CompletableFuture
                    .supplyAsync(task, this.executor)
                    .orTimeout(RequestCoalescerConstants.REQUEST_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

            newFuture.whenComplete((result, ex) -> {
                inflightRequests.remove(k);
                if (ex != null) {
                    log.error("Request failed for key: {}", k, ex);
                } else {
                    log.debug("Request completed for key: {}", k);
                }
            });

            return newFuture;
        });
    }
}
