package com.milesight.beaveriot.delayedqueue.component;

import com.milesight.beaveriot.context.api.DelayedSchedulerProvider;
import com.milesight.beaveriot.context.model.delayedqueue.DelayedQueue;
import com.milesight.beaveriot.context.model.delayedqueue.DelayedTask;
import com.milesight.beaveriot.delayedqueue.DelayedQueueManager;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * @author Luxb
 * @date 2025/11/26 10:08
 **/
@Component
public class DelayedScheduler implements DelayedSchedulerProvider {
    private static final String DELAYED_QUEUE_NAME = "system-delayed-scheduler";
    private final DelayedQueueManager delayedQueueManager;

    public DelayedScheduler(DelayedQueueManager delayedQueueManager) {
        this.delayedQueueManager = delayedQueueManager;
    }

    public void schedule(Duration delay, Runnable runnable) {
        if (delay == null) {
            throw new IllegalArgumentException("Delay cannot be null");
        }

        if (runnable == null) {
            throw new IllegalArgumentException("Runnable cannot be null");
        }

        DelayedQueue<Void> delayedQueue = delayedQueueManager.getDelayedQueue(DELAYED_QUEUE_NAME);
        String topic = UUID.randomUUID().toString();
        DelayedTask<Void> delayedTask = DelayedTask.of(topic, null, delay);
        delayedQueue.registerConsumer(topic, task -> {
            if (task.getId().equals(delayedTask.getId())) {
                runnable.run();
            }
        }, true);
        delayedQueue.offer(delayedTask);
    }
}
