package com.milesight.beaveriot.eventbus.api;

import lombok.Getter;

import java.util.LinkedHashMap;

/**
 * @author leon
 */
@Getter
public class EventResponse extends LinkedHashMap<String,Object> {

    public EventResponse add(EventResponse eventResponse){
        if (eventResponse != null){
            this.putAll(eventResponse);
        }
        return this;
    }

    public static EventResponse of(String key, Object value){
        EventResponse eventResponse = new EventResponse();
        eventResponse.put(key,value);
        return eventResponse;
    }
    public static EventResponse empty() {
        return new EventResponse();
    }
}
