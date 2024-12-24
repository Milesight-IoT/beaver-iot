package com.milesight.beaveriot.eventbus;

import com.milesight.beaveriot.base.constants.StringConstant;
import com.milesight.beaveriot.base.utils.KeyPatternMatcher;
import com.milesight.beaveriot.eventbus.enums.EventSource;
import lombok.Data;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author leon
 */
@Data
public class ListenerCacheKey {

    private String payloadKey;

    private String eventType;

    private EventSource[] eventSources;

    public ListenerCacheKey(String payloadKey, String eventType, EventSource[] eventSource) {
        this.payloadKey = payloadKey;
        this.eventType = eventType;
        this.eventSources = eventSource;
    }

    public String[] matchMultiKeys(String payloadMultiKeys) {
        return Arrays.stream(payloadMultiKeys.split(StringConstant.COMMA)).filter(key->KeyPatternMatcher.match(payloadKey.trim(), key.trim())).toArray(String[]::new);
    }

    public boolean matchEventType(String payloadEventType) {
        if (!StringUtils.hasText(eventType)) {
            return true;
        }
        return eventType.equals(payloadEventType);
    }

    public boolean matchEventSource(EventSource eventSource) {
        if (ObjectUtils.isEmpty(eventSources)) {
            return true;
        }
        return ArrayUtils.contains(eventSources, eventSource);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ListenerCacheKey that = (ListenerCacheKey) o;
        return Objects.equals(payloadKey, that.payloadKey) && Objects.equals(eventType, that.eventType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payloadKey, eventType);
    }

}
