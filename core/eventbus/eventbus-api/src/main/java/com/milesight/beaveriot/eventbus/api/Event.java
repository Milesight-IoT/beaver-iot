package com.milesight.beaveriot.eventbus.api;


/**
 * @author leon
 */
public interface Event<T extends IdentityKey> {

    String getEventType();

    default String getPayloadKey() {
        return getPayload().getKey();
    }

    void setPayload(IdentityKey payload);

    void setEventType(String eventType);

    T getPayload();

}
