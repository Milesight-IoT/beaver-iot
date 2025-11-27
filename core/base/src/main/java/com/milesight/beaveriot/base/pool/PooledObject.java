package com.milesight.beaveriot.base.pool;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wrapper for pooled objects with metadata
 *
 * @author Luxb
 * @date 2025/11/27
 */
public class PooledObject<T> {
    @Getter
    private final T object;
    private final long createdAt;
    private volatile long lastUsedAt;
    private final AtomicBoolean isInUse;

    public PooledObject(T object) {
        this.object = object;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.lastUsedAt = now;
        this.isInUse = new AtomicBoolean(false);
    }

    public void updateLastUsedAt() {
        this.lastUsedAt = System.currentTimeMillis();
    }

    public boolean isInUse() {
        return isInUse.get();
    }

    /**
     * Mark this object as in use
     *
     * @return true if successfully marked, false if already in use
     */
    public boolean markInUse() {
        return isInUse.compareAndSet(false, true);
    }

    /**
     * Mark this object as available
     */
    public void markAvailable() {
        isInUse.set(false);
        updateLastUsedAt();
    }

    /**
     * Get idle time in milliseconds
     */
    public long getIdleTimeMillis() {
        return System.currentTimeMillis() - lastUsedAt;
    }

    @Override
    public String toString() {
        return "PooledObject{" +
                "object=" + object.getClass().getSimpleName() +
                ", createdTime=" + createdAt +
                ", lastUsedTime=" + lastUsedAt +
                ", inUse=" + isInUse.get() +
                ", idleTime=" + getIdleTimeMillis() + "ms" +
                '}';
    }
}
