package com.milesight.beaveriot.coalescer.redis;

import com.milesight.beaveriot.coalescer.RequestCoalescer;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
public class RedisRequestCoalescer<V> implements RequestCoalescer<V> {

    private final RedissonClient redissonClient;

    private final Executor executor;

    private static final String KEY_PREFIX = "request-coalescer:";
    private static final String CURRENT_PREFIX = "current:";
    private static final String RESULT_PREFIX = "result:";
    private static final String TOPIC_PREFIX = "topic:";

    private static final Duration CURRENT_LEASE_TIME = Duration.ofSeconds(30);
    private static final Duration RESULT_TTL = Duration.ofMinutes(1);
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(30);

    public RedisRequestCoalescer(RedissonClient redissonClient, Executor executor) {
        this.redissonClient = redissonClient;
        this.executor = executor;
        log.info("Created RedisRequestCoalescer with distributed coordination");
    }

    @Override
    public CompletableFuture<V> executeAsync(String key, Supplier<V> task) {
        String currentKey = getCurrentKey(key);
        String resultKey = getResultKey(key);
        String topicName = getTopicName(key);

        CompletableFuture<V> future = new CompletableFuture<>();

        try {
            boolean acquired = redissonClient.getBucket(currentKey).setIfAbsent(1, CURRENT_LEASE_TIME);

            if (acquired) {
                log.debug("acquired for key: {}, executing task", key);
                redissonClient.getBucket(resultKey).delete();
                executeTask(key, CompletableFuture.supplyAsync(task, this.executor), resultKey, topicName, future, currentKey);
            } else {
                log.debug("not acquired for key: {}, waiting for result", key);
                waitForResult(key, resultKey, topicName, future);
            }
        } catch (Exception e) {
            log.error("Error in executeAsync for key: {}", key, e);
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * Execute the actual task and store result in Redis.
     */
    private void executeTask(String key, CompletableFuture<V> task,
                             String resultKey, String topicName,
                             CompletableFuture<V> future, String currentKey) {
        task.whenComplete((result, error) -> {
            try {
                TaskResult<V> taskResult;
                if (error != null) {
                    log.debug("Task failed for key: {}", key, error);
                    taskResult = TaskResult.error(error);
                } else {
                    log.debug("Task succeeded for key: {}", key);
                    taskResult = TaskResult.success(result);
                }

                // Store result in Redis
                redissonClient.getBucket(resultKey).set(taskResult, RESULT_TTL);

                // Publish notification
                RTopic topic = redissonClient.getTopic(topicName);
                topic.publish("completed");

                // Complete local future
                if (error != null) {
                    future.completeExceptionally(error);
                } else {
                    future.complete(result);
                }
            } catch (Exception e) {
                log.error("Error storing result for key: {}", key, e);
                future.completeExceptionally(e);
            } finally {
                redissonClient.getBucket(currentKey).delete();
                log.debug("Released for key: {}", key);
            }
        });
    }

    /**
     * Wait for task result from Redis.
     */
    private void waitForResult(String key, String resultKey, String topicName, CompletableFuture<V> future) {
        // First check if result already exists
        TaskResult<V> existingResult = getResultFromRedis(resultKey);
        if (existingResult != null) {
            log.debug("Found existing result for key: {}", key);
            completeFromTaskResult(future, existingResult);
            return;
        }

        RTopic topic = redissonClient.getTopic(topicName);

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(WAIT_TIMEOUT.toMillis());
                if (!future.isDone()) {
                    log.warn("Timeout waiting for result for key: {}", key);
                    future.completeExceptionally(
                            new RuntimeException("Timeout waiting for result")
                    );
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Listen for result
        int listenerId = topic.addListener(String.class, (channel, msg) -> {
            log.debug("Received notification for key: {}", key);
            TaskResult<V> result = getResultFromRedis(resultKey);
            if (result != null && !future.isDone()) {
                completeFromTaskResult(future, result);
            }
        });

        // Remove listener when future completes
        future.whenComplete((result, error) -> topic.removeListener(listenerId));
    }

    /**
     * Get task result from Redis.
     */
    @SuppressWarnings("unchecked")
    private TaskResult<V> getResultFromRedis(String resultKey) {
        try {
            Object obj = redissonClient.getBucket(resultKey).get();
            if (obj instanceof TaskResult) {
                return (TaskResult<V>) obj;
            }
        } catch (Exception e) {
            log.error("Error retrieving result from Redis: {}", resultKey, e);
        }
        return null;
    }

    /**
     * Complete future from TaskResult.
     */
    private void completeFromTaskResult(CompletableFuture<V> future, TaskResult<V> taskResult) {
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

    // Key generation methods
    private String getCurrentKey(String key) {
        return KEY_PREFIX + CURRENT_PREFIX + key;
    }

    private String getResultKey(String key) {
        return KEY_PREFIX + RESULT_PREFIX + key;
    }

    private String getTopicName(String key) {
        return KEY_PREFIX + TOPIC_PREFIX + key;
    }
}
