package com.milesight.beaveriot.context.model.delayedqueue;

import com.milesight.beaveriot.context.i18n.locale.LocaleContext;
import com.milesight.beaveriot.context.security.TenantContext;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * author: Luxb
 * create: 2025/11/13 9:19
 **/
@Data
public class DelayedTask<T> implements Delayed {
    private String topic;
    private T payload;
    private Long delayTime;

    @Setter(lombok.AccessLevel.NONE)
    private String id;
    @Setter(lombok.AccessLevel.NONE)
    private long expireTime;
    @Setter(lombok.AccessLevel.NONE)
    private Map<ContextKey, Object> context = new HashMap<>();

    private DelayedTask() {
        this.initContext();
    }

    private DelayedTask(String id, String topic, T payload, Duration delayDuration) {
        this();

        Assert.notNull(topic, "topic cannot be null");
        Assert.notNull(delayDuration, "delayDuration cannot be null");

        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.topic = topic;
        this.payload = payload;
        this.setDelayDuration(delayDuration);
    }

    public static <T> DelayedTask<T> of(String topic, T payload, Duration delayDuration) {
        return of(null, topic, payload, delayDuration);
    }

    public static <T> DelayedTask<T> of(String id, String topic, T payload, Duration delayDuration) {
        return new DelayedTask<>(id, topic, payload, delayDuration);
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

    private void initContext() {
        this.context.put(ContextKey.TENANT, TenantContext.tryGetTenantId().orElse(null));
        this.context.put(ContextKey.LOCALE, LocaleContext.getLocale());
    }

    public enum ContextKey {
        TENANT,
        LOCALE
    }
}