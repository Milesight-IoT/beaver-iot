package com.milesight.beaveriot.dashboard.event;

import com.milesight.beaveriot.dashboard.dto.DashboardDTO;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.IdentityKey;

/**
 * DashboardEvent class.
 *
 * @author simon
 * @date 2025/9/28
 */
public class DashboardEvent implements Event<DashboardDTO> {
    private DashboardDTO dashboardDTO;

    private String eventType;

    public DashboardEvent() {}

    public DashboardEvent(DashboardDTO dashboardDTO, String eventType) {
        this.dashboardDTO = dashboardDTO;
        this.eventType = eventType;
    }

    public static DashboardEvent of(DashboardDTO dashboardDTO, String eventType) {
        return new DashboardEvent(dashboardDTO, eventType);
    }

    @Override
    public String getEventType() {
        return this.eventType;
    }

    @Override
    public void setPayload(IdentityKey payload) {
        this.dashboardDTO = (DashboardDTO) payload;
    }

    @Override
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public DashboardDTO getPayload() {
        return this.dashboardDTO;
    }

    public static class EventType {

        private EventType() {
        }

        public static final String CREATED = "Created";
        public static final String UPDATED = "Updated";
        public static final String DELETED = "Deleted";
    }
}
