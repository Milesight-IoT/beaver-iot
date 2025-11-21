package com.milesight.beaveriot.context.model.delayedqueue;

import com.milesight.beaveriot.context.i18n.locale.LocaleContext;
import com.milesight.beaveriot.context.security.TenantContext;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * author: Luxb
 * create: 2025/11/13 9:19
 **/
@Getter
public class DelayedTask<T> implements Delayed {
    private String id;
    private String topic;
    @Setter
    private T payload;
    private Long delayTime;
    private long expireTime;
    private final AtomicLong requeueCount;
    private final Map<String, Object> context;

    private DelayedTask() {
        this.requeueCount = new AtomicLong(0);
        this.context = new ConcurrentHashMap<>();
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

    public void setTopic(String topic) {
        Assert.notNull(topic, "topic cannot be null");

        this.topic = topic;
    }

    public void setDelayDuration(Duration delayDuration) {
        Assert.notNull(delayDuration, "delayDuration cannot be null");

        setDelayTime(delayDuration.toMillis());
    }

    private void setDelayTime(long delayTime) {
        this.delayTime = delayTime;
        renew();
    }

    @Override
    public long getDelay(@NonNull TimeUnit unit) {
        long remainingTime = expireTime - System.currentTimeMillis();
        return remainingTime < 0 ? 0 : unit.convert(remainingTime, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NonNull Delayed other) {
        if (!(other instanceof DelayedTask<?> that)) {
            throw new ClassCastException("Cannot compare DelayedTask with " + other.getClass());
        }

        return Long.compare(this.expireTime, that.expireTime);
    }

    private void initContext() {
        putContextValue(ContextKey.TENANT, TenantContext.tryGetTenantId().orElse(null));
        putContextValue(ContextKey.LOCALE, LocaleContext.getLocale());
    }

    public enum ContextKey {
        TENANT,
        LOCALE
    }

    public void putContextValue(ContextKey key, Object value) {
        if (value == null) {
            return;
        }

        context.put(getInnerContextKey(key), value);
    }

    public Object getContextValue(ContextKey key) {
        return context.get(getInnerContextKey(key));
    }

    public void incrementRequeueCount() {
        requeueCount.incrementAndGet();
    }

    public long getRequeueCount() {
        return requeueCount.get();
    }

    private String getInnerContextKey(ContextKey key) {
        return Constants.INNER_CONTEXT_KEY_PREFIX + key.name();
    }

    private static class Constants {
        public static final String INNER_CONTEXT_KEY_PREFIX = "_INNER_";
    }
}