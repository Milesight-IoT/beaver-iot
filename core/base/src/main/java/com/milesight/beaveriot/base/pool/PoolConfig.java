package com.milesight.beaveriot.base.pool;

import lombok.Getter;

import java.time.Duration;

/**
 * Configuration for ObjectPool
 *
 * @author Luxb
 * @date 2025/11/27
 */
@Getter
public class PoolConfig {
    /**
     * Minimum number of objects to keep in the pool
     */
    private final int minIdle;
    /**
     * Maximum number of objects that can be created
     */
    private final int maxTotal;
    /**
     * Maximum time an idle object can stay in the pool before being evicted
     */
    private final Duration maxIdleTime;
    /**
     * How often to run the eviction check
     */
    private final Duration evictionCheckInterval;
    /**
     * Maximum time to wait when borrowing an object if pool is exhausted
     */
    private final Duration maxWaitTime;

    private PoolConfig(Builder builder) {
        this.minIdle = builder.minIdle;
        this.maxTotal = builder.maxTotal;
        this.maxIdleTime = builder.maxIdleTime;
        this.evictionCheckInterval = builder.evictionCheckInterval;
        this.maxWaitTime = builder.maxWaitTime;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int minIdle = 2;
        private int maxTotal = 10;
        private Duration maxIdleTime = Duration.ofMinutes(5);
        private Duration evictionCheckInterval = Duration.ofMinutes(1);
        private Duration maxWaitTime = Duration.ofSeconds(30);

        public Builder minIdle(int minIdle) {
            if (minIdle < 0) {
                throw new IllegalArgumentException("minIdle must be >= 0");
            }
            this.minIdle = minIdle;
            return this;
        }

        public Builder maxTotal(int maxTotal) {
            if (maxTotal <= 0) {
                throw new IllegalArgumentException("maxTotal must be > 0");
            }
            this.maxTotal = maxTotal;
            return this;
        }

        public Builder maxIdleTime(Duration maxIdleTime) {
            if (maxIdleTime == null || maxIdleTime.isNegative()) {
                throw new IllegalArgumentException("maxIdleTime must be positive");
            }
            this.maxIdleTime = maxIdleTime;
            return this;
        }

        public Builder evictionCheckInterval(Duration evictionCheckInterval) {
            if (evictionCheckInterval == null || evictionCheckInterval.isNegative()) {
                throw new IllegalArgumentException("evictionCheckInterval must be positive");
            }
            this.evictionCheckInterval = evictionCheckInterval;
            return this;
        }

        public Builder maxWaitTime(Duration maxWaitTime) {
            if (maxWaitTime == null || maxWaitTime.isNegative()) {
                throw new IllegalArgumentException("maxWaitTime must be positive");
            }
            this.maxWaitTime = maxWaitTime;
            return this;
        }

        public PoolConfig build() {
            if (minIdle > maxTotal) {
                throw new IllegalArgumentException("minIdle cannot be greater than maxTotal");
            }
            return new PoolConfig(this);
        }
    }
}