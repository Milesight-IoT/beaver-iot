package com.milesight.beaveriot.eventbus;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author leon
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class UniqueListenerCacheKey extends ListenerCacheKey{

    private String id;

    public UniqueListenerCacheKey(String id, String payloadKey, String eventType) {
        super(payloadKey, eventType);
        this.id = id;
    }

}