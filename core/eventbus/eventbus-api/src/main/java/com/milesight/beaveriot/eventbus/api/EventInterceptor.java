package com.milesight.beaveriot.eventbus.api;

/**
 * @author leon
 */
public interface EventInterceptor<T extends Event<? extends IdentityKey>> {

    /**
     * Called before the event is handled.
     * @param event
     * @return true if the event should be handled, false to skip handling
     */
    default boolean beforeHandle(T event) {
        return true;
    }

    /**
     * Called after the event is handled.
     * @param event
     * @param response
     * @param exception if any exception occurred during handling
     * @throws Exception
     */
    default void afterHandle(T event, EventResponse response, Exception exception) throws Exception {
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Check if the interceptor should be applied to the given event.
     * @param event
     * @return
     */
    boolean match(T event);

}
