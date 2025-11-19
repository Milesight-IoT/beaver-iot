package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.model.delayedqueue.DelayedQueue;

/**
 * Service provider interface for managing and retrieving delayed queue instances.
 *
 * <p>This interface serves as the primary entry point for obtaining {@link DelayedQueue} instances
 * in the BeaverIoT system. It provides a centralized mechanism to access delayed queues by name,
 * enabling different components to either share the same queue instance or maintain separate queues
 * for different business purposes.
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li><b>Named Queue Management</b> - Queues are identified and accessed by unique string names</li>
 *   <li><b>Type-Safe Generics</b> - Each queue can be parameterized with specific payload types</li>
 *   <li><b>Singleton Pattern</b> - The same queue name consistently returns the same queue instance</li>
 *   <li><b>Multi-Tenant Support</b> - Queues are typically scoped within tenant contexts</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @Autowired
 * private DelayedQueueServiceProvider delayedQueueProvider;
 *
 * public void scheduleDeviceCommand() {
 *     // Obtain a delayed queue for device commands
 *     DelayedQueue<DeviceCommand> queue = delayedQueueProvider.getDelayedQueue("device-commands");
 *
 *     // Create and submit a delayed task
 *     DelayedTask<DeviceCommand> task = DelayedTask.of(
 *         "command-topic",
 *         new DeviceCommand("turn-on"),
 *         Duration.ofMinutes(5)
 *     );
 *     queue.offer(task);
 *
 *     // Register a consumer to process expired tasks
 *     queue.registerConsumer("command-topic", delayedTask -> {
 *         DeviceCommand cmd = delayedTask.getPayload();
 *         executeCommand(cmd);
 *     });
 * }
 * }</pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>Implementations of this interface must be thread-safe. Multiple threads may safely invoke
 * {@link #getDelayedQueue(String)} concurrently with identical or different queue names without
 * external synchronization.
 *
 * <h2>Implementation Considerations:</h2>
 * <ul>
 *   <li>Queue instances are typically cached and reused for identical queue names</li>
 *   <li>Underlying implementations may vary: in-memory queues, distributed queues (e.g., Redis-based)</li>
 *   <li>Queue lifecycle is managed by the Spring IoC container</li>
 *   <li>Queue instances may be shared across multiple tenants or isolated per tenant depending on configuration</li>
 * </ul>
 *
 * @author Luxb
 * @since 1.0
 * @see DelayedQueue
 * @see com.milesight.beaveriot.context.model.delayedqueue.DelayedTask
 */
public interface DelayedQueueServiceProvider {

    /**
     * Retrieves or creates a delayed queue instance identified by the specified queue name.
     *
     * <p>This method returns a {@link DelayedQueue} instance corresponding to the given queue name.
     * If a queue with the specified name already exists, the existing instance is returned (singleton behavior).
     * Otherwise, a new queue is instantiated, cached, and returned for subsequent calls.
     *
     * <p>The returned queue is parameterized by the generic type {@code T}, which defines the type
     * of payload objects that tasks within this queue will carry. This enables compile-time type safety
     * when working with queue operations.
     *
     * <h3>Queue Naming Best Practices:</h3>
     * <ul>
     *   <li>Use descriptive, kebab-case names: {@code "device-commands"}, {@code "data-sync-tasks"}</li>
     *   <li>Incorporate business domain or feature context: {@code "notification-delivery"}, {@code "alarm-cleanup"}</li>
     *   <li>Avoid special characters except hyphens ({@code -}) and underscores ({@code _})</li>
     *   <li>Keep names concise yet meaningful to facilitate maintenance and debugging</li>
     * </ul>
     *
     * <h3>Usage Examples:</h3>
     * <pre>{@code
     * // Obtain a queue for notification delivery tasks
     * DelayedQueue<Notification> notificationQueue =
     *     provider.getDelayedQueue("notification-delivery");
     *
     * // Obtain a queue for alarm cleanup operations
     * DelayedQueue<AlarmCleanupTask> cleanupQueue =
     *     provider.getDelayedQueue("alarm-cleanup");
     *
     * // Generic usage with Map payload
     * DelayedQueue<Map<String, Object>> genericQueue =
     *     provider.getDelayedQueue("generic-events");
     * }</pre>
     *
     * @param <T>       the type of payload that tasks in this queue will contain
     * @param queueName the unique identifier for the queue; must not be {@code null} or empty.
     *                  Identical queue names always return the same queue instance.
     * @return a {@link DelayedQueue} instance for the specified queue name; never {@code null}
     * @throws IllegalArgumentException if {@code queueName} is {@code null}, empty, or contains invalid characters
     * @throws RuntimeException         if queue creation fails due to infrastructure issues
     *                                  (e.g., connection failure to Redis in distributed mode)
     */
    <T> DelayedQueue<T> getDelayedQueue(String queueName);
}
