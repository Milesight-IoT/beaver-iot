package com.milesight.beaveriot.delayedqueue.component

import com.milesight.beaveriot.context.model.delayedqueue.DelayedQueue
import com.milesight.beaveriot.context.model.delayedqueue.DelayedTask
import com.milesight.beaveriot.delayedqueue.DelayedQueueManager
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

/**
 * Unit tests for DelayedScheduler
 *
 * @author Luxb
 * @date 2025/11/26 16:49
 */
class DelayedSchedulerTest extends Specification {

    DelayedQueueManager mockDelayedQueueManager
    DelayedQueue<Void> mockDelayedQueue
    DelayedScheduler delayedScheduler

    def setup() {
        mockDelayedQueueManager = Mock(DelayedQueueManager)
        mockDelayedQueue = Mock(DelayedQueue)
        mockDelayedQueueManager.getDelayedQueue("system-delayed-scheduler") >> mockDelayedQueue
        delayedScheduler = new DelayedScheduler(mockDelayedQueueManager)
    }

    // ==================== Constructor tests ====================

    def "constructor should initialize with DelayedQueueManager"() {
        given:
        def newMockManager = Mock(DelayedQueueManager)

        when:
        def scheduler = new DelayedScheduler(newMockManager)

        then:
        scheduler != null
        noExceptionThrown()
    }

    // ==================== schedule() - validation tests ====================

    def "schedule should throw IllegalArgumentException when delay is null"() {
        given:
        Runnable runnable = Mock(Runnable)

        when:
        delayedScheduler.schedule(null, runnable)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Delay cannot be null"
        0 * mockDelayedQueue.registerConsumer(_, _, _)
        0 * mockDelayedQueue.offer(_)
    }

    def "schedule should throw IllegalArgumentException when runnable is null"() {
        given:
        Duration delay = Duration.ofSeconds(1)

        when:
        delayedScheduler.schedule(delay, null)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Runnable cannot be null"
        0 * mockDelayedQueue.registerConsumer(_, _, _)
        0 * mockDelayedQueue.offer(_)
    }

    // ==================== schedule() - basic functionality tests ====================

    def "schedule should get delayed queue with correct queue name"() {
        given:
        Duration delay = Duration.ofSeconds(1)
        Runnable runnable = Mock(Runnable)

        when:
        delayedScheduler.schedule(delay, runnable)

        then:
        1 * mockDelayedQueueManager.getDelayedQueue("system-delayed-scheduler") >> mockDelayedQueue
    }

    def "schedule should register consumer with oneTime flag set to true"() {
        given:
        Duration delay = Duration.ofSeconds(1)
        Runnable runnable = Mock(Runnable)
        String capturedTopic = null
        boolean capturedOneTime = false

        when:
        delayedScheduler.schedule(delay, runnable)

        then:
        1 * mockDelayedQueue.registerConsumer(_ as String, _ as Consumer<DelayedTask<Void>>, _) >> { String topic, Consumer consumer, boolean oneTime ->
            capturedTopic = topic
            capturedOneTime = oneTime
            return "consumer-id"
        }
        1 * mockDelayedQueue.offer(_)
        capturedTopic != null
        capturedOneTime
    }

    def "schedule should create DelayedTask with null payload"() {
        given:
        Duration delay = Duration.ofSeconds(1)
        Runnable runnable = Mock(Runnable)
        DelayedTask<Void> capturedTask = null

        when:
        delayedScheduler.schedule(delay, runnable)

        then:
        1 * mockDelayedQueue.registerConsumer(_, _, _)
        1 * mockDelayedQueue.offer(_ as DelayedTask<Void>) >> { DelayedTask<Void> task ->
            capturedTask = task
        }
        capturedTask != null
        capturedTask.payload == null
    }

    def "schedule should create DelayedTask with correct delay"() {
        given:
        Duration delay = Duration.ofMillis(500)
        Runnable runnable = Mock(Runnable)
        DelayedTask<Void> capturedTask = null
        long beforeSchedule = System.currentTimeMillis()

        when:
        delayedScheduler.schedule(delay, runnable)
        long afterSchedule = System.currentTimeMillis()

        then:
        1 * mockDelayedQueue.registerConsumer(_, _, _)
        1 * mockDelayedQueue.offer(_ as DelayedTask<Void>) >> { DelayedTask<Void> task ->
            capturedTask = task
        }
        capturedTask != null
        // The task should expire approximately after the delay (with generous buffer for test stability)
        capturedTask.expireTime >= beforeSchedule + delay.toMillis()
        capturedTask.expireTime <= afterSchedule + delay.toMillis()
    }

    def "schedule should generate unique topic for each call"() {
        given:
        Duration delay = Duration.ofSeconds(1)
        Runnable runnable = Mock(Runnable)
        Set<String> topics = new HashSet<>()

        when:
        10.times {
            delayedScheduler.schedule(delay, runnable)
        }

        then:
        10 * mockDelayedQueue.registerConsumer(_ as String, _ as Consumer<DelayedTask<Void>>, _) >> { String topic, Consumer consumer, boolean oneTime ->
            topics.add(topic)
            return "consumer-id"
        }
        10 * mockDelayedQueue.offer(_)
        topics.size() == 10
        topics.every { it != null && it.length() > 0 }
    }

    def "schedule should offer task to delayed queue"() {
        given:
        Duration delay = Duration.ofSeconds(1)
        Runnable runnable = Mock(Runnable)

        when:
        delayedScheduler.schedule(delay, runnable)

        then:
        1 * mockDelayedQueue.offer(_)
    }

    // ==================== schedule() - consumer behavior tests ====================

    def "schedule should register consumer that checks task ID before running"() {
        given:
        Duration delay = Duration.ofSeconds(1)
        AtomicBoolean runnableCalled = new AtomicBoolean(false)
        Runnable runnable = { -> runnableCalled.set(true) }

        Consumer<DelayedTask<Void>> capturedConsumer = null
        DelayedTask<Void> capturedTask = null

        when:
        delayedScheduler.schedule(delay, runnable)

        then:
        1 * mockDelayedQueue.registerConsumer(_ as String, _ as Consumer<DelayedTask<Void>>, _) >> { String topic, Consumer consumer, boolean oneTime ->
            capturedConsumer = consumer
            return "consumer-id"
        }
        1 * mockDelayedQueue.offer(_ as DelayedTask<Void>) >> { DelayedTask<Void> task ->
            capturedTask = task
        }
        capturedConsumer != null
        capturedTask != null

        when:
        // Simulate consumer being called with the correct task
        capturedConsumer.accept(capturedTask)

        then:
        runnableCalled.get()
    }

    // ==================== schedule() - integration tests ====================

    def "schedule should handle multiple concurrent schedules"() {
        given:
        Duration delay = Duration.ofMillis(100)
        CountDownLatch latch = new CountDownLatch(5)
        AtomicInteger runCount = new AtomicInteger(0)

        def consumers = []
        def tasks = []

        when:
        5.times {
            delayedScheduler.schedule(delay, {
                runCount.incrementAndGet()
                latch.countDown()
            })
        }

        then:
        5 * mockDelayedQueue.registerConsumer(_ as String, _ as Consumer<DelayedTask<Void>>, _) >> { String topic, Consumer consumer, boolean oneTime ->
            consumers.add(consumer)
            return "consumer-id-${consumers.size()}"
        }
        5 * mockDelayedQueue.offer(_ as DelayedTask<Void>) >> { DelayedTask<Void> task ->
            tasks.add(task)
        }
        consumers.size() == 5
        tasks.size() == 5

        when:
        // Simulate all tasks being consumed
        consumers.eachWithIndex { consumer, index ->
            consumer.accept(tasks[index])
        }
        latch.await(1, TimeUnit.SECONDS)

        then:
        runCount.get() == 5
    }

    def "schedule should handle different delay durations"() {
        given:
        Runnable runnable = Mock(Runnable)
        def delays = [
                Duration.ofMillis(1),
                Duration.ofMillis(100),
                Duration.ofSeconds(1),
                Duration.ofMinutes(1),
                Duration.ofHours(1)
        ]

        when:
        delays.each { delay ->
            delayedScheduler.schedule(delay, runnable)
        }

        then:
        delays.size() * mockDelayedQueue.registerConsumer(_, _, true)
        delays.size() * mockDelayedQueue.offer(_)
    }

    def "schedule should handle runnable that throws exception"() {
        given:
        Duration delay = Duration.ofSeconds(1)
        Runnable runnable = { -> throw new RuntimeException("Test exception") }

        Consumer<DelayedTask<Void>> capturedConsumer = null
        DelayedTask<Void> capturedTask = null

        when:
        delayedScheduler.schedule(delay, runnable)

        then:
        1 * mockDelayedQueue.registerConsumer(_ as String, _ as Consumer<DelayedTask<Void>>, _) >> { String topic, Consumer consumer, boolean oneTime ->
            capturedConsumer = consumer
            return "consumer-id"
        }
        1 * mockDelayedQueue.offer(_ as DelayedTask<Void>) >> { DelayedTask<Void> task ->
            capturedTask = task
        }
        notThrown(Exception)

        when:
        // Simulate consumer being called
        capturedConsumer.accept(capturedTask)

        then:
        thrown(RuntimeException)
    }
}
