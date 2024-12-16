package com.milesight.beaveriot.rule.components.eventlistener;

import org.apache.camel.spi.Metadata;

/**
 * @author leon
 */
public class EventListenerConstants {

    @Metadata(description = "The fired time", javaType = "string")
    public static final String HEADER_EVENTBUS_TYPE = "CamelEventBusType";
    @Metadata(description = "The timestamp of the message", javaType = "string")
    public static final String HEADER_EVENTBUS_PAYLOAD_KEY = "CamelEventBusPayloadKey";

    private EventListenerConstants() {
    }

}
