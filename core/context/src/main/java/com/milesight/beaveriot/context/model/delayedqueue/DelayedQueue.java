package com.milesight.beaveriot.context.model.delayedqueue;

import java.util.function.Consumer;

/**
 * author: Luxb
 * create: 2025/11/13 9:18
 **/
public interface DelayedQueue<T> {
    /**
     * Adds a new delayed task to the queue, or updates the delay (i.e., reschedules) of an existing task
     * if another task with the same {@code taskId} is already present.
     *
     * <p>The task will become available for consumption only after its delay has elapsed from the time
     * it was offered (or last updated).
     *
     * @param task the delayed task to add or update; must not be null and must have a non-null {@code taskId}
     */
    void offer(DelayedTask<T> task);
    /**
     * Cancels a previously scheduled task if it has not yet been processed.
     *
     * @param taskId the unique identifier of the task to cancel; must not be null
     */
    void cancel(String taskId);
    /**
     * Registers a consumer to receive delayed tasks associated with a specific topic.
     * The consumer will be invoked when tasks matching the topic become ready (i.e., their delay has expired).
     *
     * @param topic     the topic used to route tasks to this consumer; must not be null
     * @param consumer  the callback to invoke when a ready task is available; must not be null
     * @return a unique consumer ID that can be used to unregister this consumer later
     */
    String registerConsumer(String topic, Consumer<DelayedTask<T>> consumer);
    /**
     * Registers a consumer to receive delayed tasks associated with a specific topic.
     * The consumer will be invoked when tasks matching the topic become ready (i.e., their delay has expired).
     *
     * @param topic       the topic used to route tasks to this consumer; must not be null
     * @param consumer    the callback to invoke when a ready task is available; must not be null
     * @param isConsumeOnce if true, the consumer will be unregistered after the first task is consumed
     * @return a unique consumer ID that can be used to unregister this consumer later
     */
    String registerConsumer(String topic, Consumer<DelayedTask<T>> consumer, boolean isConsumeOnce);
    /**
     * Unregisters all consumers associated with the given topic.
     *
     * @param topic the topic whose consumers should be removed; must not be null
     */
    void unregisterConsumer(String topic);
    /**
     * Unregisters a specific consumer identified by both topic and consumer ID.
     *
     * @param topic       the topic under which the consumer was registered; must not be null
     * @param consumerId  the unique ID of the consumer to remove; must not be null
     */
    void unregisterConsumer(String topic, String consumerId);
}