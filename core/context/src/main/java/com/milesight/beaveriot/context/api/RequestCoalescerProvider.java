package com.milesight.beaveriot.context.api;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * RequestCoalescerProvider
 *
 * @author simon
 * @date 2025/12/2
 */
public interface RequestCoalescerProvider {
    Object execute(String key, Supplier<Object> task);

    CompletableFuture<Object> executeAsync(String key, Supplier<Object> task);
}
