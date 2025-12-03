package com.milesight.beaveriot.delayedqueue;

import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.context.i18n.locale.LocaleContext;
import com.milesight.beaveriot.context.model.delayedqueue.DelayedQueue;
import com.milesight.beaveriot.context.model.delayedqueue.DelayedTask;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.context.support.SpringContext;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.spring.aop.ScopedLockConfiguration;
import org.redisson.RedissonShutdownException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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
    protected volatile Map<String, Map<String, DelayedConsumer<T>>> topicDelayedConsumersMap;
    protected ExecutorService listenerExecutor;
    protected ExecutorService consumerExecutor;
    protected final AtomicBoolean isListening;
    protected final AtomicLong listenerStartTime;

    public BaseDelayedQueue(@NonNull String queueName, @NonNull DelayedQueueWrapper<T> delayQueue, @NonNull Map<String, Long> taskExpireTimeMap) {
        this.queueName = queueName;
        this.delayQueue = delayQueue;
        this.taskExpireTimeMap = taskExpireTimeMap;
        this.topicDelayedConsumersMap = new ConcurrentHashMap<>();
        this.isListening = new AtomicBoolean(false);
        this.listenerStartTime = new AtomicLong();
        if (!this.delayQueue.isEmpty()) {
            CompletableFuture.delayedExecutor(Constants.LISTENER_START_DELAY_TIME.toSeconds(), TimeUnit.SECONDS).execute(this::startListener);
        }
    }

    @Override
    public void offer(DelayedTask<T> task) {
        validateTask(task);

        startListener();
        doWithLock(task.getId(), () -> {
            Long existingExpireTime = taskExpireTimeMap.put(task.getId(), task.renew().getExpireTime());
            delayQueue.offer(task);
            if (existingExpireTime == null) {
                log.debug("Delayed queue '{}' offered task '{}'", queueName, task.getId());
            } else {
                log.debug("Delayed queue '{}' renewed task '{}'", queueName, task.getId());
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
            log.debug("Delayed queue '{}' cancelled task '{}'", queueName, taskId);
        }
    }

    private void requeue(DelayedTask<T> task) {
        doWithLock(task.getId(), () -> {
            taskExpireTimeMap.putIfAbsent(task.getId(), task.getExpireTime());
            delayQueue.offer(task);
        });
    }

    private DelayedTask<T> doTake() throws InterruptedException {
        while (true) {
            DelayedTask<T> task = delayQueue.take();

            if (isReallyExpired(task)) {
                return task;
            }
        }
    }

    @Override
    public String registerConsumer(String topic, Consumer<DelayedTask<T>> consumer) {
        return registerConsumer(topic, consumer, false);
    }

    @Override
    public String registerConsumer(String topic, Consumer<DelayedTask<T>> consumer, boolean isConsumeOnce) {
        Map<String, DelayedConsumer<T>> delayedConsumers = topicDelayedConsumersMap.computeIfAbsent(topic, k -> new ConcurrentHashMap<>());
        String consumerId = UUID.randomUUID().toString();
        delayedConsumers.put(consumerId, DelayedConsumer.of(consumerId, consumer, isConsumeOnce));
        startListener();
        return consumerId;
    }

    @Override
    public void unregisterConsumer(String topic) {
        topicDelayedConsumersMap.remove(topic);
    }

    @Override
    public void unregisterConsumer(String topic, String consumerId) {
        Map<String, DelayedConsumer<T>> consumers = topicDelayedConsumersMap.get(topic);
        if (CollectionUtils.isEmpty(consumers)) {
            return;
        }

        consumers.remove(consumerId);
        if (CollectionUtils.isEmpty(consumers)) {
            topicDelayedConsumersMap.remove(topic);
        }
    }

    private void startListener() {
        if (isListening.get()) {
            return;
        }

        doWithLock(this::doStartListener);
    }

    private void doStartListener() {
        if (isListening.compareAndSet(false, true)) {
            listenerExecutor = Executors.newSingleThreadExecutor(runnable -> {
                Thread t = new Thread(runnable, Constants.LISTENER_THREAD_NAME_PREFIX + queueName);
                t.setDaemon(false);
                return t;
            });

            consumerExecutor = Executors.newCachedThreadPool(runnable -> {
                Thread t = new Thread(runnable, Constants.CONSUMER_THREAD_NAME_PREFIX + queueName);
                t.setDaemon(false);
                return t;
            });

            listenerStartTime.set(System.currentTimeMillis());
            listenerExecutor.execute(() -> {
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        DelayedTask<T> task = doTake();

                        if (task.getTopic() == null) {
                            continue;
                        }

                        Map<String, DelayedConsumer<T>> delayedConsumers = topicDelayedConsumersMap.get(task.getTopic());
                        if (CollectionUtils.isEmpty(delayedConsumers)) {
                            if (System.currentTimeMillis() - listenerStartTime.get() > Constants.TASK_RETENTION_PERIOD.toMillis()) {
                                if (task.getRequeueCount() >= Constants.MAX_REQUEUE_COUNT) {
                                    log.warn("Task '{}' has reached max requeue count and will be discarded because there is still no consumer registered for topic '{}' in queue '{}'",
                                            task.getId(), task.getTopic(), queueName);
                                    continue;
                                }
                            }

                            task.incrementRequeueCount();
                            CompletableFuture.delayedExecutor(Constants.REQUEUE_DELAY_TIME.toSeconds(), TimeUnit.SECONDS).execute(() -> requeue(task));
                            continue;
                        }

                        log.debug("Delayed queue '{}' consumed task '{}'", queueName, task.getId());
                        delayedConsumers.forEach((consumerId, delayedConsumer) -> CompletableFuture.runAsync(() -> {
                            try {
                                initConsumerContext(task);
                                delayedConsumer.getConsumer().accept(task);
                                if (delayedConsumer.isConsumeOnce()) {
                                    unregisterConsumer(task.getTopic(), consumerId);
                                }
                            } catch (Exception e) {
                                log.error("Error occurred while consuming task '{}' by consumer '{}' for queue '{}'", task.getId(), consumerId, queueName, e);
                            }
                        }, consumerExecutor));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Delayed queue listener interrupted, shutting down for queue '{}'", queueName);
                        break;
                    } catch (RedissonShutdownException e) {
                        log.warn("Redisson is shutdown, shutting down for queue '{}'", queueName);
                        break;
                    } catch (Exception e) {
                        log.error("Error occurred while consuming task", e);
                    }
                }
            });
        }
    }

    private void initConsumerContext(DelayedTask<T> task) {
        String tenantId = (String) task.getContextValue(DelayedTask.ContextKey.TENANT);
        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
        }
        Locale locale = (Locale) task.getContextValue(DelayedTask.ContextKey.LOCALE);
        if (locale != null) {
            LocaleContext.setLocale(locale);
        }
    }

    private boolean isReallyExpired(DelayedTask<T> task) {
        return task != null && taskExpireTimeMap.remove(task.getId(), task.getExpireTime());
    }

    private void validateTask(DelayedTask<T> task) {
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

    private void doWithLock(Runnable runnable) {
        doWithLock(null, runnable);
    }

    private void doWithLock(String taskId, Runnable runnable) {
        String lockName;
        if (taskId == null) {
            lockName = MessageFormat.format(Constants.LOCK_NAME_DELAYED_QUEUE_HANDLE_QUEUE_FORMAT, queueName);
        } else {
            lockName = MessageFormat.format(Constants.LOCK_NAME_DELAYED_QUEUE_HANDLE_TASK_FORMAT, queueName, taskId);
        }
        ScopedLockConfiguration lockConfiguration = ScopedLockConfiguration.builder(LockScope.GLOBAL)
                .name(lockName)
                .lockAtLeastFor(Duration.ofMinutes(0))
                .lockAtMostFor(Duration.ofMinutes(30))
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
        doWithLock(this::doDestroy);
    }

    private void doDestroy() {
        isListening.set(false);

        if (listenerExecutor != null || consumerExecutor != null) {
            log.debug("Shutting down delayed queue listener and consumer executor for queue '{}'", queueName);
        }
        try {
            shutdownExecutor(listenerExecutor, true);
            shutdownExecutor(consumerExecutor, false);

            if (listenerExecutor != null && !listenerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Delayed queue listener did not terminate within 5 seconds for queue '{}'", queueName);
            }
            if (consumerExecutor != null && !consumerExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                shutdownExecutor(consumerExecutor, true);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            shutdownExecutor(listenerExecutor, true);
            shutdownExecutor(consumerExecutor, true);
        }
        if (listenerExecutor != null || consumerExecutor != null) {
            log.debug("Delayed queue listener and consumer executor shut down for queue '{}'", queueName);
        }
        listenerExecutor = null;
        consumerExecutor = null;
    }

    private void shutdownExecutor(ExecutorService executor, boolean force) {
        if (executor != null) {
            if (force) {
                executor.shutdownNow();
            } else {
                executor.shutdown();
            }
        }
    }

    @Data
    protected static class DelayedConsumer<T> {
        private String consumerId;
        private Consumer<DelayedTask<T>> consumer;
        private boolean isConsumeOnce;

        private DelayedConsumer(String consumerId, Consumer<DelayedTask<T>> consumer, boolean isConsumeOnce) {
            Assert.notNull(consumerId, "Consumer id cannot be null");
            Assert.notNull(consumer, "Consumer cannot be null");

            this.consumerId = consumerId;
            this.consumer = consumer;
            this.isConsumeOnce = isConsumeOnce;
        }

        public static <T> DelayedConsumer<T> of(String consumerId, Consumer<DelayedTask<T>> consumer, boolean isConsumeOnce) {
            return new DelayedConsumer<>(consumerId, consumer, isConsumeOnce);
        }
    }

    private static class Constants {
        public static final String LOCK_NAME_DELAYED_QUEUE_HANDLE_QUEUE_FORMAT = "delayed-queue:{0}:handle-queue";
        public static final String LOCK_NAME_DELAYED_QUEUE_HANDLE_TASK_FORMAT = "delayed-queue:{0}:handle-task:{1}";
        public static final String LISTENER_THREAD_NAME_PREFIX = "DelayedQueue-Listener-";
        public static final String CONSUMER_THREAD_NAME_PREFIX = "DelayedQueue-Consumer-";
        public static final Duration LISTENER_START_DELAY_TIME = Duration.ofMinutes(5);
        public static final Duration TASK_RETENTION_PERIOD = Duration.ofMinutes(3);
        public static final long MAX_REQUEUE_COUNT = 3;
        public static final Duration REQUEUE_DELAY_TIME = Duration.ofSeconds(1);
    }
}