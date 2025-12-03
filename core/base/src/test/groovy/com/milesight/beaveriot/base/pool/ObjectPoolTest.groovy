package com.milesight.beaveriot.base.pool

import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Function

/**
 * @author Luxb
 * @date 2025/12/3 9:31
 */
class ObjectPoolTest extends Specification {

    // ==================== initialization tests ====================

    def "pool should initialize with minIdle objects"() {
        given:
        def config = PoolConfig.builder()
                .minIdle(3)
                .maxTotal(10)
                .build()

        when:
        def pool = ObjectPool.newPool(config, { new StringBuilder() }, StringBuilder.class)

        then:
        def stats = pool.getStatistics()
        stats.totalObjects() == 3
        stats.idleObjects() == 3
        stats.activeObjects() == 0

        cleanup:
        pool?.destroy()
    }

    // ==================== execute tests ====================

    def "execute with function should return result and return object to pool"() {
        given:
        def config = PoolConfig.builder()
                .minIdle(1)
                .maxTotal(5)
                .build()
        def pool = ObjectPool.newPool(config, { new StringBuilder() }, StringBuilder.class)

        when:
        def result = pool.execute({ sb ->
            sb.append("Hello")
            sb.toString()
        } as Function)

        then:
        result == "Hello"
        def stats = pool.getStatistics()
        stats.idleObjects() == 1
        stats.activeObjects() == 0

        cleanup:
        pool?.destroy()
    }

    def "execute with consumer should complete successfully"() {
        given:
        def config = PoolConfig.builder()
                .minIdle(1)
                .maxTotal(5)
                .build()
        def pool = ObjectPool.newPool(config, { new AtomicInteger(0) }, AtomicInteger.class)

        when:
        pool.execute({ counter ->
            counter.incrementAndGet()
        } as Consumer)

        then:
        def stats = pool.getStatistics()
        stats.idleObjects() == 1
        stats.activeObjects() == 0

        cleanup:
        pool?.destroy()
    }

    // ==================== pool limit tests ====================

    def "pool should respect maxTotal limit and timeout when exhausted"() {
        given:
        def config = PoolConfig.builder()
                .minIdle(1)
                .maxTotal(2)
                .maxWaitTime(Duration.ofMillis(100))
                .build()
        def pool = ObjectPool.newPool(config, { new StringBuilder() }, StringBuilder.class)

        when:
        def latch = new CountDownLatch(2)
        def timeoutOccurred = false

        Thread.start {
            pool.execute({ sb ->
                latch.countDown()
                Thread.sleep(500)
            } as Consumer)
        }
        Thread.start {
            pool.execute({ sb ->
                latch.countDown()
                Thread.sleep(500)
            } as Consumer)
        }

        latch.await(1, TimeUnit.SECONDS)

        try {
            pool.execute({ sb -> sb.append("test") } as Consumer)
        } catch (Exception ignored) {
            timeoutOccurred = true
        }

        then:
        timeoutOccurred

        cleanup:
        pool?.destroy()
    }

    // ==================== eviction tests ====================

    def "pool should evict idle objects exceeding maxIdleTime"() {
        given:
        def config = PoolConfig.builder()
                .minIdle(1)
                .maxTotal(5)
                .maxIdleTime(Duration.ofMillis(100))
                .evictionCheckInterval(Duration.ofMillis(150))
                .build()
        def pool = ObjectPool.newPool(config, { new StringBuilder() }, StringBuilder.class)

        when:
        pool.execute({ sb -> sb.append("1") } as Consumer)
        pool.execute({ sb -> sb.append("2") } as Consumer)
        pool.execute({ sb -> sb.append("3") } as Consumer)

        Thread.sleep(400)

        then:
        def stats = pool.getStatistics()
        stats.totalObjects() == 1
        stats.idleObjects() == 1

        cleanup:
        pool?.destroy()
    }

    // ==================== destroy tests ====================

    def "pool should invoke destructor when destroying objects"() {
        given:
        def destroyCount = new AtomicInteger(0)
        def config = PoolConfig.builder()
                .minIdle(2)
                .maxTotal(5)
                .build()
        def pool = ObjectPool.newPool(config,
                { new StringBuilder() },
                { sb -> destroyCount.incrementAndGet() },
                StringBuilder.class)

        when:
        pool.destroy()

        then:
        destroyCount.get() == 2
    }

    def "pool should throw exception when executing on closed pool"() {
        given:
        def config = PoolConfig.builder()
                .minIdle(1)
                .maxTotal(5)
                .build()
        def pool = ObjectPool.newPool(config, { new StringBuilder() }, StringBuilder.class)
        pool.destroy()

        when:
        pool.execute({ sb -> sb.append("test") } as Consumer)

        then:
        def e = thrown(Exception)
        e instanceof IllegalStateException || (e.cause instanceof IllegalStateException)
    }

    // ==================== statistics tests ====================

    def "pool statistics should track object states correctly"() {
        given:
        def config = PoolConfig.builder()
                .minIdle(2)
                .maxTotal(10)
                .build()
        def pool = ObjectPool.newPool(config, { new StringBuilder() }, StringBuilder.class)

        expect:
        def stats1 = pool.getStatistics()
        stats1.totalObjects() == 2
        stats1.idleObjects() == 2
        stats1.activeObjects() == 0
        stats1.maxTotal() == 10

        when:
        def latch = new CountDownLatch(1)
        def completeLatch = new CountDownLatch(1)
        Thread.start {
            pool.execute({ sb ->
                latch.countDown()
                completeLatch.await(2, TimeUnit.SECONDS)
            } as Consumer)
        }
        latch.await(1, TimeUnit.SECONDS)

        then:
        def stats2 = pool.getStatistics()
        stats2.totalObjects() == 2
        stats2.activeObjects() == 1

        cleanup:
        completeLatch?.countDown()
        pool?.destroy()
    }

    // ==================== concurrency tests ====================

    def "pool should handle concurrent access safely"() {
        given:
        def config = PoolConfig.builder()
                .minIdle(2)
                .maxTotal(5)
                .build()
        def pool = ObjectPool.newPool(config, { new AtomicInteger(0) }, AtomicInteger.class)
        def successCount = new AtomicInteger(0)
        def threads = []

        when:
        10.times {
            threads << Thread.start {
                try {
                    pool.execute({ counter ->
                        counter.incrementAndGet()
                        Thread.sleep(50)
                    } as Consumer)
                    successCount.incrementAndGet()
                } catch (Exception ignored) {
                    // Some operations may timeout
                }
            }
        }

        threads*.join()

        then:
        successCount.get() > 0

        cleanup:
        pool?.destroy()
    }
}
