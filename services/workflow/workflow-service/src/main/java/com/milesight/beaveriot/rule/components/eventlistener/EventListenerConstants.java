package com.milesight.beaveriot.rule.components.eventlistener;

import org.apache.camel.spi.Metadata;

/**
 * @author leon
 */
public class EventListenerConstants {

    @Metadata(description = "eventbus type", javaType = "string")
    public static final String HEADER_EVENTBUS_TYPE = "CamelEventBusType";
    @Metadata(description = "eventbus payload key", javaType = "string")
    public static final String HEADER_EVENTBUS_PAYLOAD_KEY = "CamelEventBusPayloadKey";

    private EventListenerConstants() {
    }

}
