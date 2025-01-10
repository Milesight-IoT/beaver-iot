package com.milesight.beaveriot.context.integration.model.event;


import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.IdentityKey;

/**
 * @author leon
 */
public class EntityEvent implements Event<Entity> {

    private Entity entity;
    private String eventType;
    public EntityEvent() {
    }

    public EntityEvent(String eventType, Entity entity) {
        this.eventType = eventType;
        this.entity = entity;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public void setPayload(IdentityKey payload) {
        this.entity = (Entity) payload;
    }

    @Override
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public Entity getPayload() {
        return entity;
    }

    public static EntityEvent of(String eventType, Entity entity) {
        return new EntityEvent(eventType, entity);
    }

    public static class EventType {
        private EventType() {
        }

        public static final String CREATED = "Created";
        public static final String UPDATED = "Updated";
        public static final String DELETED = "Deleted";
    }
}
