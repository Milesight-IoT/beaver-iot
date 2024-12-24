package com.milesight.beaveriot.eventbus;

import com.milesight.beaveriot.eventbus.enums.EventSource;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author leon
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class UniqueListenerCacheKey extends ListenerCacheKey{

    private String id;

    public UniqueListenerCacheKey(String id, String payloadKey, String eventType, EventSource[] eventSources) {
        super(payloadKey, eventType, eventSources);
        this.id = id;
    }

}
