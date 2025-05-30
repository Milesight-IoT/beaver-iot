package com.milesight.beaveriot.context.integration.model.event;


import com.milesight.beaveriot.context.integration.model.DeviceTemplate;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.IdentityKey;

/**
 * @author leon
 */
public class DeviceTemplateEvent implements Event<DeviceTemplate> {

    private DeviceTemplate deviceTemplate;
    private String eventType;

    public DeviceTemplateEvent() {
    }

    public DeviceTemplateEvent(String eventType, DeviceTemplate deviceTemplate) {
        this.eventType = eventType;
        this.deviceTemplate = deviceTemplate;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public void setPayload(IdentityKey payload) {
        this.deviceTemplate = (DeviceTemplate) payload;
    }

    @Override
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public DeviceTemplate getPayload() {
        return deviceTemplate;
    }

    public static DeviceTemplateEvent of(String eventType, DeviceTemplate device) {
        return new DeviceTemplateEvent(eventType, device);
    }

    public static class EventType {

        private EventType() {
        }

        public static final String CREATED = "Created";
        public static final String UPDATED = "Updated";
        public static final String DELETED = "Deleted";
    }

}
