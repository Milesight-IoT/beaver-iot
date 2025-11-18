package com.milesight.beaveriot.delayedqueue;

import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.context.model.delayedqueue.DelayedQueue;
import com.milesight.beaveriot.context.model.delayedqueue.DelayedTask;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.context.support.SpringContext;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.spring.aop.ScopedLockConfiguration;
import org.redisson.RedissonShutdownException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * author: Luxb
 * create: 2025/11/13 13:52
 **/
@Slf4j
public class BaseDelayedQueue<T> implements DelayedQueue<T>, DisposableBean {
    private static final LockProvider lockProvider = SpringContext.getBean(LockProvider.class);
    protected final String queueName;
    protected DelayedQueueWrapper<T> delayQueue;
    protected Map<String, Long> taskExpireTimeMap;
    protected Map<String, Map<String, Consumer<DelayedTask<T>>>> topicConsumersMap;
    protected ExecutorService listenerExecutor;
    protected final AtomicBoolean isListening;

    public BaseDelayedQueue(String queueName) {
        this.queueName = queueName;
        this.topicConsumersMap = new ConcurrentHashMap<>();
        this.isListening = new AtomicBoolean(false);
    }

    @Override
    public void offer(DelayedTask<T> task) {
        validateTask(task);

        doWithLock(task.getId(), () -> {
            Long existingExpireTime = taskExpireTimeMap.put(task.getId(), task.renew().getExpireTime());
            delayQueue.offer(task);
            if (existingExpireTime == null) {
                log.debug("Delayed queue {} offered task: {}", queueName, task.getId());
            } else {
                log.debug("Delayed queue {} renewed task: {}", queueName, task.getId());
            }
        });
    }

    @Override
    public void cancel(String taskId) {
        if (taskId == null) {
            return;
        }

        Long expireTime = taskExpireTimeMap.remove(taskId);
        if (expireTime != null) {
            log.debug("Delayed queue {} cancelled task: {}", queueName, taskId);
        }
    }

    private DelayedTask<T> doTake() throws InterruptedException {
        while (true) {
            DelayedTask<T> task = delayQueue.take();

            if (isReallyExpired(task)) {
                log.debug("Delayed queue {} consumed task: {}", queueName, task.getId());
                return task;
            }
        }
    }

    @Override
    public String registerConsumer(String topic, Consumer<DelayedTask<T>> consumer) {
        Map<String, Consumer<DelayedTask<T>>> consumers = topicConsumersMap.computeIfAbsent(topic, k -> new ConcurrentHashMap<>());
        String consumerId = UUID.randomUUID().toString();
        consumers.put(consumerId, consumer);
        this.startListener();
        return consumerId;
    }

    @Override
    public void unregisterConsumer(String topic) {
        topicConsumersMap.remove(topic);
    }

    @Override
    public void unregisterConsumer(String topic, String consumerId) {
        Map<String, Consumer<DelayedTask<T>>> consumers = topicConsumersMap.get(topic);
        if (CollectionUtils.isEmpty(consumers)) {
            return;
        }

        consumers.remove(consumerId);
    }

    private void startListener() {
        if (isListening.compareAndSet(false, true)) {
            listenerExecutor = Executors.newSingleThreadExecutor(runnable -> {
                Thread t = new Thread(runnable, Constants.LISTENER_THREAD_NAME_PREFIX + queueName);
                t.setDaemon(false);
                return t;
            });

            String tenantId = TenantContext.tryGetTenantId().orElse(null);
            listenerExecutor.execute(() -> {
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                }

                while(isListening.get() && !Thread.currentThread().isInterrupted()) {
                    try {
                        DelayedTask<T> task = doTake();

                        if (task.getTopic() == null) {
                            continue;
                        }

                        Map<String, Consumer<DelayedTask<T>>> consumers = topicConsumersMap.get(task.getTopic());
                        if (CollectionUtils.isEmpty(consumers)) {
                            continue;
                        }

                        consumers.forEach((consumerId, consumer) -> {
                            try {
                                consumer.accept(task);
                            } catch (Exception e) {
                                log.error("Error occurred while consuming task: {}, consumerId: {}", task.getId(), consumerId, e);
                            }
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Delayed queue listener interrupted, shutting down for queue: {}", queueName);
                        break;
                    } catch (RedissonShutdownException e) {
                        log.warn("Redisson is shutdown, shutting down for queue: {}", queueName);
                        break;
                    } catch (Exception e) {
                        log.error("Error occurred while consuming task", e);
                    }
                }
            });
        }
    }

    private boolean isReallyExpired(DelayedTask<T> task) {
        return taskExpireTimeMap.remove(task.getId(), task.getExpireTime());
    }

    public void validateTask(DelayedTask<T> task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }

        if (task.getId() == null) {
            throw new IllegalArgumentException("Task id cannot be null");
        }

        if (task.getDelayTime() == null) {
            throw new IllegalArgumentException("Task delay time cannot be null");
        }

        if (task.getTopic() == null) {
            throw new IllegalArgumentException("Task topic cannot be null");
        }
    }

    protected void doWithLock(String taskId, Runnable runnable) {
        ScopedLockConfiguration lockConfiguration = ScopedLockConfiguration.builder(LockScope.GLOBAL)
                .name(MessageFormat.format(Constants.LOCK_NAME_DELAYED_QUEUE_HANDLE_TASK_FORMAT, queueName, taskId))
                .lockAtLeastFor(Duration.ofMinutes(0))
                .lockAtMostFor(Duration.ofMinutes(10))
                .throwOnLockFailure(false)
                .build();

        lockProvider.lock(lockConfiguration).ifPresentOrElse(lock -> {
            try {
                runnable.run();
            } finally {
                lock.unlock();
            }
        }, () -> {
            throw new RuntimeException("Another task is running, skipping this task");
        });
    }

    @Override
    public void destroy() {
        log.debug("Shutting down delayed queue listener executor for queue: {}", queueName);
        if (listenerExecutor == null) {
            return;
        }

        isListening.set(false);
        listenerExecutor.shutdown();
        try {
            if (!listenerExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                listenerExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            listenerExecutor.shutdownNow();
        }
    }

    private static class Constants {
        public static final String LOCK_NAME_DELAYED_QUEUE_HANDLE_TASK_FORMAT = "delayed-queue:{0}:handle-task:{1}";
        public static final String LISTENER_THREAD_NAME_PREFIX = "DelayedQueue-Listener-";
    }
}