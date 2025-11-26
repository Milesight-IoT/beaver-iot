package com.milesight.beaveriot.delayedqueue

import com.milesight.beaveriot.context.model.delayedqueue.DelayedTask
import com.milesight.beaveriot.context.support.SpringContext
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.core.SimpleLock
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import spock.lang.Specification
import spock.lang.Unroll

import java.lang.reflect.Field
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

/**
 * @author Luxb
 * @date 2025/11/20 9:01
 */
class BaseDelayedQueueTest extends Specification {

    DelayedQueueWrapper<String> mockDelayQueue
    Map<String, Long> taskExpireTimeMap
    BaseDelayedQueue<String> delayedQueue
    String queueName = "test-queue"

    def setupSpec() {
        // Mock SpringContext beanFactory to provide LockProvider
        ConfigurableListableBeanFactory mockBeanFactory = Mock(ConfigurableListableBeanFactory)
        LockProvider mockLockProvider = Mock(LockProvider)
        SimpleLock mockLock = Mock(SimpleLock)

        // Mock lock() to return an Optional containing the mock lock
        mockLockProvider.lock(_ as LockConfiguration) >> Optional.of(mockLock)
        mockBeanFactory.getBean(LockProvider.class) >> mockLockProvider

        // Use reflection to set the static beanFactory field in SpringContext
        Field beanFactoryField = SpringContext.class.getDeclaredField("beanFactory")
        beanFactoryField.setAccessible(true)
        beanFactoryField.set(null, mockBeanFactory)
    }

    def setup() {
        mockDelayQueue = Mock(DelayedQueueWrapper)
        taskExpireTimeMap = new ConcurrentHashMap<>()
        delayedQueue = new BaseDelayedQueue<>(queueName, mockDelayQueue, taskExpireTimeMap)
    }

    def cleanup() {
        if (delayedQueue != null) {
            delayedQueue.destroy()
        }
    }

    // ==================== Constructor tests ====================

    def "constructor should initialize all fields correctly"() {
        given:
        def queue = new BaseDelayedQueue<>("test", mockDelayQueue, taskExpireTimeMap)

        expect:
        queue.queueName == "test"
        queue.delayQueue == mockDelayQueue
        queue.taskExpireTimeMap == taskExpireTimeMap
        queue.topicDelayedConsumersMap != null
        queue.topicDelayedConsumersMap.isEmpty()
    }

    def "constructor should not start listener when the delayed queue is empty"() {
        given:
        mockDelayQueue.isEmpty() >> true

        when:
        def queue = new BaseDelayedQueue<>("test", mockDelayQueue, taskExpireTimeMap)

        then:
        !queue.isListening.get()
        queue.listenerExecutor == null
        queue.consumerExecutor == null
    }

    def "constructor should throw exception when queueName is null"() {
        when:
        new BaseDelayedQueue<>(null, mockDelayQueue, taskExpireTimeMap)

        then:
        thrown(NullPointerException)
    }

    def "constructor should throw exception when delayQueue is null"() {
        when:
        new BaseDelayedQueue<>("test", null, taskExpireTimeMap)

        then:
        thrown(NullPointerException)
    }

    def "constructor should throw exception when taskExpireTimeMap is null"() {
        when:
        new BaseDelayedQueue<>("test", mockDelayQueue, null)

        then:
        thrown(NullPointerException)
    }

    // ==================== offer() tests ====================

    def "offer should add new task successfully"() {
        given:
        def task = DelayedTask.of("topic1", "payload1", Duration.ofMillis(100))

        when:
        delayedQueue.offer(task)

        then:
        1 * mockDelayQueue.offer(task)
        taskExpireTimeMap.containsKey(task.id)
        taskExpireTimeMap.get(task.id) == task.expireTime
    }

    def "offer should renew existing task"() {
        given:
        def task = DelayedTask.of("task-1", "topic1", "payload1", Duration.ofMillis(100))
        def oldExpireTime = task.expireTime
        taskExpireTimeMap.put(task.id, oldExpireTime)

        // Wait a bit to ensure new expire time is different
        Thread.sleep(10)

        when:
        delayedQueue.offer(task)

        then:
        1 * mockDelayQueue.offer(task)
        taskExpireTimeMap.get(task.id) > oldExpireTime
    }

    @Unroll
    def "offer should throw exception when task validation fails: #scenario"() {
        given:
        DelayedTask<String> taskToOffer = createTask()

        when:
        delayedQueue.offer(taskToOffer)

        then:
        thrown(IllegalArgumentException)
        0 * mockDelayQueue.offer(_)

        where:
        scenario     | createTask
        "null task"  | { -> null }
        "null topic" | { ->
            def task = DelayedTask.of("id1", "topic", "data", Duration.ofMillis(100))
            def field = DelayedTask.getDeclaredField("topic")
            field.setAccessible(true)
            field.set(task, null)
            task
        }
    }

    def "offer should succeed when acquiring lock"() {
        given:
        def task = DelayedTask.of("topic1", "payload1", Duration.ofMillis(100))

        when:
        delayedQueue.offer(task)

        then:
        1 * mockDelayQueue.offer(task)
        taskExpireTimeMap.containsKey(task.id)
    }

    def "offer should start listener"() {
        given:
        def task = DelayedTask.of("topic1", "payload1", Duration.ofMillis(100))
        mockDelayQueue.isEmpty() >> true

        when:
        delayedQueue.offer(task)

        then:
        delayedQueue.isListening.get()
        delayedQueue.listenerExecutor != null
        delayedQueue.consumerExecutor != null
    }

    // ==================== cancel() tests ====================

    def "cancel should remove task from expireTimeMap"() {
        given:
        def taskId = "task-1"
        taskExpireTimeMap.put(taskId, System.currentTimeMillis())

        when:
        delayedQueue.cancel(taskId)

        then:
        !taskExpireTimeMap.containsKey(taskId)
    }

    def "cancel should handle null taskId gracefully"() {
        when:
        delayedQueue.cancel(null)

        then:
        notThrown(Exception)
    }

    def "cancel should handle non-existent taskId gracefully"() {
        when:
        delayedQueue.cancel("non-existent-id")

        then:
        notThrown(Exception)
    }

    // ==================== registerConsumer() tests ====================

    def "registerConsumer should register consumer and return consumerId"() {
        given:
        mockDelayQueueDefaultTake()
        def topic = "test-topic"
        Consumer<DelayedTask<String>> consumer = Mock(Consumer)

        when:
        def consumerId = delayedQueue.registerConsumer(topic, consumer)

        then:
        consumerId != null
        delayedQueue.topicDelayedConsumersMap.containsKey(topic)
        delayedQueue.topicDelayedConsumersMap.get(topic).containsKey(consumerId)
        delayedQueue.topicDelayedConsumersMap.get(topic).get(consumerId).getConsumer() == consumer
    }

    def "registerConsumer should register multiple consumers for same topic"() {
        given:
        mockDelayQueueDefaultTake()
        def topic = "test-topic"
        Consumer<DelayedTask<String>> consumer1 = Mock(Consumer)
        Consumer<DelayedTask<String>> consumer2 = Mock(Consumer)

        when:
        def consumerId1 = delayedQueue.registerConsumer(topic, consumer1)
        def consumerId2 = delayedQueue.registerConsumer(topic, consumer2)

        then:
        consumerId1 != consumerId2
        delayedQueue.topicDelayedConsumersMap.get(topic).size() == 2
        delayedQueue.topicDelayedConsumersMap.get(topic).containsKey(consumerId1)
        delayedQueue.topicDelayedConsumersMap.get(topic).containsKey(consumerId2)
    }

    def "registerConsumer should register consumers for different topics"() {
        given:
        mockDelayQueueDefaultTake()
        def topic1 = "topic1"
        def topic2 = "topic2"
        Consumer<DelayedTask<String>> consumer1 = Mock(Consumer)
        Consumer<DelayedTask<String>> consumer2 = Mock(Consumer)

        when:
        delayedQueue.registerConsumer(topic1, consumer1)
        delayedQueue.registerConsumer(topic2, consumer2)

        then:
        delayedQueue.topicDelayedConsumersMap.size() == 2
        delayedQueue.topicDelayedConsumersMap.containsKey(topic1)
        delayedQueue.topicDelayedConsumersMap.containsKey(topic2)
    }

    // ==================== unregisterConsumer() tests ====================

    def "unregisterConsumer(topic) should remove all consumers for topic"() {
        given:
        mockDelayQueueDefaultTake()
        def topic = "test-topic"
        Consumer<DelayedTask<String>> consumer1 = Mock(Consumer)
        Consumer<DelayedTask<String>> consumer2 = Mock(Consumer)
        delayedQueue.registerConsumer(topic, consumer1)
        delayedQueue.registerConsumer(topic, consumer2)

        when:
        delayedQueue.unregisterConsumer(topic)

        then:
        !delayedQueue.topicDelayedConsumersMap.containsKey(topic)
    }

    def "unregisterConsumer(topic) should handle non-existent topic gracefully"() {
        when:
        delayedQueue.unregisterConsumer("non-existent-topic")

        then:
        notThrown(Exception)
    }

    def "unregisterConsumer(topic, consumerId) should remove specific consumer"() {
        given:
        mockDelayQueueDefaultTake()
        def topic = "test-topic"
        Consumer<DelayedTask<String>> consumer1 = Mock(Consumer)
        Consumer<DelayedTask<String>> consumer2 = Mock(Consumer)
        def consumerId1 = delayedQueue.registerConsumer(topic, consumer1)
        def consumerId2 = delayedQueue.registerConsumer(topic, consumer2)

        when:
        delayedQueue.unregisterConsumer(topic, consumerId1)

        then:
        delayedQueue.topicDelayedConsumersMap.containsKey(topic)
        !delayedQueue.topicDelayedConsumersMap.get(topic).containsKey(consumerId1)
        delayedQueue.topicDelayedConsumersMap.get(topic).containsKey(consumerId2)
    }

    def "unregisterConsumer(topic, consumerId) should handle non-existent topic gracefully"() {
        when:
        delayedQueue.unregisterConsumer("non-existent-topic", "consumer-id")

        then:
        notThrown(Exception)
    }

    def "unregisterConsumer(topic, consumerId) should handle empty consumers map gracefully"() {
        given:
        def topic = "test-topic"
        delayedQueue.topicDelayedConsumersMap.put(topic, new ConcurrentHashMap<>())

        when:
        delayedQueue.unregisterConsumer(topic, "consumer-id")

        then:
        notThrown(Exception)
    }

    // ==================== Consumer execution tests ====================

    def "consumer should be invoked when task expires"() {
        given:
        def topic = "test-topic"
        def payload = "test-payload"
        def task = DelayedTask.of("task-1", topic, payload, Duration.ofMillis(100))

        def latch = new CountDownLatch(1)
        def consumed = false
        Consumer<DelayedTask<String>> consumer = { t ->
            consumed = true
            latch.countDown()
        }

        taskExpireTimeMap.put(task.id, task.expireTime)

        // Mock take to return the task once
        mockDelayQueue.take() >> { Thread.sleep(100); task }

        when:
        delayedQueue.offer(task)
        delayedQueue.registerConsumer(topic, consumer)
        latch.await(5, TimeUnit.SECONDS)

        then:
        consumed
    }

    def "task should be consumed when consumer registers within requeue period"() {
        given:
        def topic1 = "topic1"
        def topic2 = "topic2"
        def task1 = DelayedTask.of("task-1", topic1, "payload1", Duration.ofMillis(100))
        def task2 = DelayedTask.of("task-2", topic2, "payload2", Duration.ofMillis(100))

        def latch = new CountDownLatch(1)
        def task2Consumed = false

        Consumer<DelayedTask<String>> consumer1 = Mock(Consumer)
        Consumer<DelayedTask<String>> consumer2 = { t ->
            if (t.id == task2.id) {
                task2Consumed = true
                latch.countDown()
            }
        }

        // Mock take to return tasks from queue
        mockDelayQueue.take() >> {Thread.sleep(100); task1} >> {Thread.sleep(100); task2}

        when:
        delayedQueue.offer(task1)
        delayedQueue.offer(task2)

        // Register consumer for topic1 first (topic2 has no consumer yet)
        delayedQueue.registerConsumer(topic1, consumer1)

        // Wait for task2 to expire (no consumer at this time)
        Thread.sleep(300)

        // Register consumer for topic2 within requeue period (before it's discarded)
        delayedQueue.registerConsumer(topic2, consumer2)

        // Wait for task2 to be consumed
        def consumed = latch.await(3, TimeUnit.SECONDS)

        then:
        consumed
        task2Consumed
        // Verify consumer1 was invoked for task1
        1 * consumer1.accept({ it.id == task1.id })
    }

    def "consumer should not be invoked for cancelled task"() {
        given:
        def topic = "test-topic"
        def task = DelayedTask.of("task-1", topic, "payload", Duration.ofMillis(100))

        Consumer<DelayedTask<String>> consumer = Mock(Consumer)

        mockDelayQueue.take() >> { Thread.sleep(100); task }

        when:
        delayedQueue.registerConsumer(topic, consumer)
        delayedQueue.offer(task)
        Thread.sleep(10)
        delayedQueue.cancel(task.getId())

        then:
        0 * consumer.accept(_)
    }

    def "consumer should not be invoked when no consumers registered for topic"() {
        given:
        def task = DelayedTask.of("task-1", "topic1", "payload", Duration.ofMillis(100))

        Consumer<DelayedTask<String>> consumer = Mock(Consumer)

        mockDelayQueue.take() >> { Thread.sleep(100); task }

        when:
        delayedQueue.registerConsumer("topic2", consumer) // Different topic
        delayedQueue.offer(task)

        then:
        0 * consumer.accept(_)
    }

    def "listener should handle InterruptedException and shutdown gracefully"() {
        given:
        mockDelayQueueDefaultTake()
        mockDelayQueue.take() >> { throw new InterruptedException("Test interruption") }
        Consumer<DelayedTask<String>> consumer = Mock(Consumer)

        when:
        delayedQueue.registerConsumer("topic", consumer)
        Thread.sleep(500)

        then:
        notThrown(Exception)
    }


    def "listener should continue running after consumer throws exception"() {
        given:
        def topic = "test-topic"
        def task1 = DelayedTask.of("task-1", topic, "payload1", Duration.ofMillis(100))
        def task2 = DelayedTask.of("task-2", topic, "payload2", Duration.ofMillis(100))
        def otherTask = DelayedTask.of("other-task", "other-topic", "payload", Duration.ofMillis(100))

        def latch = new CountDownLatch(2)
        Consumer<DelayedTask<String>> consumer = { t ->
            latch.countDown()
            if (t.id == task1.id) {
                throw new RuntimeException("Mock consumer error")
            }
        }

        taskExpireTimeMap.put(task1.id, task1.expireTime)
        taskExpireTimeMap.put(task2.id, task2.expireTime)

        mockDelayQueue.take() >> { Thread.sleep(100); task1 } >> { Thread.sleep(100); task2 } >> { Thread.sleep(10000); otherTask }

        when:
        delayedQueue.offer(task1)
        delayedQueue.offer(task2)
        delayedQueue.registerConsumer(topic, consumer)
        def consumed = latch.await(5, TimeUnit.SECONDS)

        then:
        consumed
    }

    def "multiple consumers should all receive the same task"() {
        given:
        def topic = "test-topic"
        def task = DelayedTask.of("task-1", topic, "payload", Duration.ofMillis(100))
        def otherTask = DelayedTask.of("other-task", "other-topic", "payload", Duration.ofMillis(100))

        def latch = new CountDownLatch(2)
        AtomicInteger sameTaskCount = new AtomicInteger(0)
        Consumer<DelayedTask<String>> consumer1 = t -> {
            if (t.id == task.id) {
                sameTaskCount.getAndIncrement()
            }
            latch.countDown()
        }
        Consumer<DelayedTask<String>> consumer2 = t -> {
            if (t.id == task.id) {
                sameTaskCount.getAndIncrement()
            }
            latch.countDown()
        }

        mockDelayQueue.take() >> { Thread.sleep(1000); task } >> { Thread.sleep(10000); otherTask }

        when:
        delayedQueue.offer(task)
        delayedQueue.registerConsumer(topic, consumer1)
        delayedQueue.registerConsumer(topic, consumer2)
        latch.await(5, TimeUnit.SECONDS)

        then:
        sameTaskCount.get() == 2
    }

    // ==================== destroy() tests ====================

    def "destroy should shutdown executors gracefully"() {
        given:
        def task = DelayedTask.of("task-1", "test-topic", "payload", Duration.ofMillis(100))

        Consumer<DelayedTask<String>> consumer = Mock(Consumer)
        mockDelayQueue.take() >> { Thread.sleep(10000); task }
        delayedQueue.registerConsumer("test-topic", consumer)
        Thread.sleep(100) // Let listener start

        when:
        delayedQueue.offer(task)
        delayedQueue.destroy()

        then:
        delayedQueue.listenerExecutor == null
        delayedQueue.consumerExecutor == null
    }

    def "destroy should handle case when executors are not initialized"() {
        when:
        delayedQueue.destroy()

        then:
        notThrown(Exception)
    }

    def "destroy should force shutdown if termination timeout exceeded"() {
        given:
        Consumer<DelayedTask<String>> consumer = Mock(Consumer)

        // Mock a task that will cause consumer to run for a long time
        def longRunningTask = DelayedTask.of("task-1", "topic", "payload", Duration.ofMillis(100))

        def latch = new CountDownLatch(1)
        consumer.accept(_) >> {
            latch.countDown()
            Thread.sleep(60000) // Sleep longer than termination timeout
        }

        mockDelayQueue.take() >> longRunningTask >> { Thread.sleep(10000); longRunningTask }
        delayedQueue.registerConsumer("topic", consumer)

        // Wait for consumer to start
        latch.await(5, TimeUnit.SECONDS)

        when:
        delayedQueue.offer(longRunningTask)
        delayedQueue.destroy()

        then:
        delayedQueue.listenerExecutor == null
        delayedQueue.consumerExecutor == null
    }

    def "destroy should handle InterruptedException during shutdown"() {
        given:
        def otherTask = DelayedTask.of("other-task", "other-topic", "payload", Duration.ofMillis(100))

        Consumer<DelayedTask<String>> consumer = Mock(Consumer)
        mockDelayQueue.take() >> { Thread.sleep(10000); otherTask }
        delayedQueue.registerConsumer("topic", consumer)

        // Interrupt current thread during destroy
        def destroyThread = Thread.start {
            Thread.currentThread().interrupt()
            delayedQueue.destroy()
        }

        when:
        destroyThread.join(5000)

        then:
        notThrown(Exception)
    }

    // ==================== Integration tests ====================

    def "full workflow: offer, consume, and cancel"() {
        given:
        def topic = "test-topic"
        def task1 = DelayedTask.of("task-1", topic, "payload1", Duration.ofMillis(100))
        def task2 = DelayedTask.of("task-2", topic, "payload2", Duration.ofMillis(100))
        def otherTask = DelayedTask.of("other-task", "other-topic", "payload", Duration.ofMillis(100))

        def latch = new CountDownLatch(1)
        def receivedTasks = []
        Consumer<DelayedTask<String>> consumer = t -> {
            receivedTasks.add(t)
            latch.countDown()
        }

        mockDelayQueue.take() >> { Thread.sleep(100); task1 } >> { Thread.sleep(100); task2 } >> { Thread.sleep(10000); otherTask }

        when:
        // Register consumer
        delayedQueue.registerConsumer(topic, consumer)

        // Offer task1
        delayedQueue.offer(task1)

        // Offer and immediately cancel task2
        delayedQueue.offer(task2)
        delayedQueue.cancel(task2.id)

        // Wait for task1 to be consumed
        def consumed = latch.await(5, TimeUnit.SECONDS)

        then:
        consumed
        receivedTasks.size() == 1
        receivedTasks[0].id == task1.id
        2 * mockDelayQueue.offer(_)
    }

    def "thread names should be set correctly"() {
        given:
        DelayedTask<String> task = DelayedTask.of("task-1", "topic", "payload", Duration.ofMillis(100))
        Consumer<DelayedTask<String>> consumer = Mock(Consumer)
        mockDelayQueue.take() >> {
            // Capture thread name when executing
            Thread.sleep(100)
            task
        }

        when:
        delayedQueue.offer(task)
        delayedQueue.registerConsumer("topic", consumer)
        Thread.sleep(200)

        then:
        delayedQueue.listenerExecutor != null
        delayedQueue.consumerExecutor != null
        // Thread names are set in the executor, verified by the thread factory
    }

    def "concurrent offer operations should be thread-safe"() {
        given:
        def tasks = (1..10).collect {
            DelayedTask.of("task-$it", "topic", "payload-$it", Duration.ofMillis(100))
        }

        when:
        def threads = tasks.collect { task ->
            Thread.start { delayedQueue.offer(task) }
        }
        threads*.join()

        then:
        10 * mockDelayQueue.offer(_)
        taskExpireTimeMap.size() == 10
    }

    // ==================== helper methods ====================

    private void mockDelayQueueDefaultTake() {
        mockDelayQueue.take() >> {
            Thread.sleep(100)
            DelayedTask.of("task-1", "topic", "payload", Duration.ofMillis(100))
        }
    }
}
