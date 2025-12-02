package com.milesight.beaveriot.coalescer;

import com.milesight.beaveriot.context.api.RequestCoalescerProvider;
import com.milesight.beaveriot.context.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * RequestCoalescerComponent class.
 *
 * @author simon
 * @date 2025/12/2
 */
@Component
@RequiredArgsConstructor
public class RequestCoalescerComponent implements RequestCoalescerProvider {
    private final RequestCoalescer<Object> requestCoalescer;

    @Override
    public Object execute(String key, Supplier<Object> task) {
        return requestCoalescer.execute(generateTenantKey(key), task);
    }

    @Override
    public CompletableFuture<Object> executeAsync(String key, Supplier<Object> task) {
        return requestCoalescer.executeAsync(generateTenantKey(key), task);
    }

    private String generateTenantKey(String key) {
        return TenantContext.tryGetTenantId().orElse("_public") + ":" + key;
    }
}
