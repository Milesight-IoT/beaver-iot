package com.milesight.beaveriot.coalescer.redis;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.milesight.beaveriot.coalescer.RequestCoalescer;
import com.milesight.beaveriot.coalescer.RequestCoalescerConstants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis-based Request Coalescer implementation with distributed coordination.
 * <p>
 * This implementation uses Redis distributed coalescer and pub/sub to coordinate
 * request coalescing across multiple nodes in a cluster.
 * </p>
 * @param <V> Result type
 * @author simon
 */
@Slf4j
public class RedisRequestCoalescer<V> implements RequestCoalescer<V>, Closeable {

    private final RedissonClient redissonClient;

    private final Executor executor;

    private static final String KEY_PREFIX = "request-coalescer:";

    private static final String CURRENT_PREFIX = "current:";
    private static final String NOTIFICATION_TOPIC = "topic";

    private final Cache<String, Set<CompletableFuture<V>>> inflightRequest;

    private final RTopic resultTopic;

    public RedisRequestCoalescer(RedissonClient redissonClient, Executor executor) {
        this.redissonClient = redissonClient;
        this.executor = executor;
        this.inflightRequest = Caffeine
                .newBuilder()
                .expireAfterAccess(RequestCoalescerConstants.REQUEST_TIMEOUT)
                .build();
        this.resultTopic = redissonClient.getTopic(getTopicName());
        this.resultTopic.addListener(TaskResult.class, (channel, msg) -> {
            log.debug("Received notification for key: {}", msg.getKey());
            Set<CompletableFuture<V>> futures = inflightRequest.getIfPresent(msg.getKey());
            if (futures != null) {
                futures.forEach(future -> {
                    if (futures.remove(future)) {
                        completeFromTaskResult(future, msg);
                    }
                });
            } else {
                log.debug("Missing request key: {}", msg.getKey());
            }
        });
        log.info("Created RedisRequestCoalescer with distributed coordination");
    }

    @Override
    public CompletableFuture<V> executeAsync(String key, Supplier<V> task) {
        String currentKey = getCurrentKey(key);

        CompletableFuture<V> future = new CompletableFuture<V>()
                .orTimeout(RequestCoalescerConstants.REQUEST_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

        try {
            boolean acquired = redissonClient.getBucket(currentKey).setIfAbsent(1);
            inflightRequest.get(currentKey, k -> ConcurrentHashMap.newKeySet()).add(future);

            if (acquired) {
                log.debug("acquired for key: {}, executing task.", currentKey);
                redissonClient.getBucket(currentKey).expire(RequestCoalescerConstants.REQUEST_TIMEOUT);
                executeTask(CompletableFuture.supplyAsync(task, this.executor), currentKey);
            } else {
                log.debug("not acquired for key: {}, waiting for result", currentKey);
            }
        } catch (Exception e) {
            log.error("Error in executeAsync for key: {}", currentKey, e);
            future.completeExceptionally(e);
        }

        return future;
    }

    private void executeTask(CompletableFuture<V> task, String currentKey) {
        task.whenComplete((result, error) -> {
            try {
                TaskResult<V> taskResult;
                if (error != null) {
                    taskResult = TaskResult.error(currentKey, error);
                } else {
                    taskResult = TaskResult.success(currentKey, result);
                }

                this.resultTopic.publish(taskResult);
            } finally {
                redissonClient.getBucket(currentKey).delete();
                log.debug("Released for key: {}", currentKey);
            }
        });
    }

    private void completeFromTaskResult(CompletableFuture<V> future, TaskResult<V> taskResult) {
        if (future.isDone()) {
            return;
        }

        if (taskResult.isSuccess()) {
            future.complete(taskResult.getValue());
        } else {
            future.completeExceptionally(
                    new RuntimeException(
                            String.format("Task failed: %s (%s)",
                                    taskResult.getErrorMessage(),
                                    taskResult.getErrorClass())
                    )
            );
        }
    }

    private String getCurrentKey(String key) {
        return KEY_PREFIX + CURRENT_PREFIX + key;
    }

    private String getTopicName() {
        return KEY_PREFIX + NOTIFICATION_TOPIC;
    }

    @Override
    public void close() throws IOException {
        this.resultTopic.removeAllListeners();
    }
}
