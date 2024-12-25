package com.milesight.beaveriot.context.integration.model;


import java.util.Map;

/**
 * @author leon
 */
public interface EventContextAccessor {


    Map<String, Object> getContext();

    void setContext(Map<String, Object> context);

    Object getContext(String key);

    <T> T getContext(String key, T defaultValue);

    void putContext(String key, Object value);

}
