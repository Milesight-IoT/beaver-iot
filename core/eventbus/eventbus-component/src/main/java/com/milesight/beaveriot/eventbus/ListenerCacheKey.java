package com.milesight.beaveriot.eventbus;

import com.milesight.beaveriot.base.constants.StringConstant;
import com.milesight.beaveriot.base.utils.KeyPatternMatcher;
import lombok.Data;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author leon
 */
@Data
public class ListenerCacheKey {

    private String payloadKey;

    private String[] eventTypes;

    public ListenerCacheKey(String payloadKey, String[] eventType) {
        this.payloadKey = payloadKey;
        this.eventTypes = eventType;
    }

    public String[] matchMultiKeys(String payloadMultiKeys) {
        return Arrays.stream(payloadMultiKeys.split(StringConstant.COMMA)).filter(key->KeyPatternMatcher.match(payloadKey.trim(), key.trim())).toArray(String[]::new);
    }

    public boolean matchEventType(String payloadEventType) {
        if (ObjectUtils.isEmpty(eventTypes)) {
            return true;
        }
        return ArrayUtils.contains(eventTypes, payloadEventType);
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
        return Objects.equals(payloadKey, that.payloadKey) && Objects.equals(eventTypes, that.eventTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payloadKey, eventTypes);
    }

}
