package com.milesight.beaveriot.delayedqueue.model;

import lombok.Data;
import lombok.NonNull;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * author: Luxb
 * create: 2025/11/13 9:19
 **/
@Data
public class DelayedTask<T> implements Delayed {
    private final String id;
    private T payload;
    private final long expireTime;
    private final Long createdAt;

    private DelayedTask(String id, T payload, Duration delay) {
        Assert.notNull(id, "id cannot be null");

        this.id = id;
        this.payload = payload;
        this.createdAt = System.currentTimeMillis();
        this.expireTime = this.createdAt + delay.toMillis();
    }

    public static <T> DelayedTask<T> of(String taskId, T payload, Duration delay) {
        return new DelayedTask<>(taskId, payload, delay);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = expireTime - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NonNull Delayed o) {
        if (o instanceof DelayedTask) {
            return Long.compare(this.expireTime, ((DelayedTask<?>) o).getExpireTime());
        }
        return 0;
    }
}