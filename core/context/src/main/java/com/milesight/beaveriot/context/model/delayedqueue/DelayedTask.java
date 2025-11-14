package com.milesight.beaveriot.context.model.delayedqueue;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * author: Luxb
 * create: 2025/11/13 9:19
 **/
@NoArgsConstructor
@Data
public class DelayedTask<T> implements Delayed {
    private String id;
    private T payload;
    private Long delayTime;

    @Setter(lombok.AccessLevel.NONE)
    private long expireTime;

    private DelayedTask(String id, T payload, Duration delayDuration) {
        Assert.notNull(id, "id cannot be null");
        Assert.notNull(delayDuration, "delayDuration cannot be null");

        this.id = id;
        this.payload = payload;
        this.setDelayDuration(delayDuration);
    }

    public static <T> DelayedTask<T> of(String taskId, T payload, Duration delayDuration) {
        return new DelayedTask<>(taskId, payload, delayDuration);
    }

    public DelayedTask<T> renew() {
        expireTime = System.currentTimeMillis() + delayTime;
        return this;
    }

    public void setDelayDuration(Duration delayDuration) {
        setDelayTime(delayDuration.toMillis());
    }

    protected void setDelayTime(long delayTime) {
        this.delayTime = delayTime;
        renew();
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long remainingTime = expireTime - System.currentTimeMillis();
        return unit.convert(remainingTime, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NonNull Delayed other) {
        if (!(other instanceof DelayedTask<?> that)) {
            throw new ClassCastException("Cannot compare DelayedTask with " + other.getClass());
        }
        return Long.compare(this.expireTime, that.expireTime);
    }
}